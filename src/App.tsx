import React, { useState, useEffect } from 'react';
import {
  Clock,
  Plus,
  Play,
  CheckCircle,
  Link2,
  BookOpen,
  Cloud,
  Smartphone,
  Sparkles,
  Edit2,
  Trash2,
  ExternalLink,
  ChevronRight,
  Book,
  Layers,
  Award,
  Bell,
  Check,
  Search,
  Filter,
  CheckSquare,
  Square,
  HelpCircle,
  Flame,
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { StudySession, Subject, Chapter, Lecture, StudySettings } from './types';
import { INITIAL_SESSIONS, INITIAL_SETTINGS } from './defaultData';
import { VideoPlayerModal } from './components/VideoPlayerModal';
import { BulkLinkImporterModal } from './components/BulkLinkImporterModal';
import { SessionEditorModal } from './components/SessionEditorModal';
import { SubjectEditorModal } from './components/SubjectEditorModal';
import { ChapterEditorModal } from './components/ChapterEditorModal';
import { QuickNotesModal } from './components/QuickNotesModal';
import { GoogleDriveSyncModal } from './components/GoogleDriveSyncModal';
import { AndroidSourceModal } from './components/AndroidSourceModal';
import { PomodoroTimer } from './components/PomodoroTimer';
import { soundManager, isSessionActiveNow } from './utils';

const STORAGE_KEY = 'study_tracker_sessions_v2';
const SETTINGS_KEY = 'study_tracker_settings_v2';

export default function App() {
  // Load initial sessions from localStorage or defaultData
  const [sessions, setSessions] = useState<StudySession[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        return JSON.parse(saved);
      }
    } catch {
      // LocalStorage error
    }
    return INITIAL_SESSIONS;
  });

  const [settings, setSettings] = useState<StudySettings>(() => {
    try {
      const saved = localStorage.getItem(SETTINGS_KEY);
      if (saved) {
        return JSON.parse(saved);
      }
    } catch {}
    return INITIAL_SETTINGS;
  });

  // Current selected session, subject and chapter
  const [selectedSessionId, setSelectedSessionId] = useState<string>(sessions[0]?.id || 'session-1');
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>(
    sessions[0]?.subjects[0]?.id || 'sub-physics'
  );
  const [selectedChapterId, setSelectedChapterId] = useState<string>(
    sessions[0]?.subjects[0]?.chapters[0]?.id || 'chap-vector'
  );

  // Active Modals state
  const [activeVideoLecture, setActiveVideoLecture] = useState<Lecture | null>(null);
  const [showBulkImporter, setShowBulkImporter] = useState(false);
  const [showSessionEditor, setShowSessionEditor] = useState(false);
  const [editingSession, setEditingSession] = useState<StudySession | null>(null);
  const [showSubjectEditor, setShowSubjectEditor] = useState(false);
  const [editingSubject, setEditingSubject] = useState<Subject | null>(null);
  const [showChapterEditor, setShowChapterEditor] = useState(false);
  const [editingChapter, setEditingChapter] = useState<Chapter | null>(null);
  const [quickNotesTarget, setQuickNotesTarget] = useState<{
    title: string;
    subtitle: string;
    notes: string;
    onSave: (notes: string) => void;
  } | null>(null);
  const [showDriveSync, setShowDriveSync] = useState(false);
  const [showAndroidSource, setShowAndroidSource] = useState(false);
  const [filterMode, setFilterMode] = useState<'all' | 'pending' | 'completed'>('all');
  const [searchQuery, setSearchQuery] = useState('');

  // Real-time clock for current time display
  const [currentTime, setCurrentTime] = useState(new Date());

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Save to localStorage
  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
    } catch {}
  }, [sessions]);

  useEffect(() => {
    try {
      localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
    } catch {}
  }, [settings]);

  // Derived current objects
  const currentSession = sessions.find((s) => s.id === selectedSessionId) || sessions[0];
  const currentSubject =
    currentSession?.subjects.find((sub) => sub.id === selectedSubjectId) ||
    currentSession?.subjects[0];
  const currentChapter =
    currentSubject?.chapters.find((chap) => chap.id === selectedChapterId) ||
    currentSubject?.chapters[0];

  // Calculate stats
  const totalLecturesInChapter = currentChapter?.lectures.length || 0;
  const completedLecturesInChapter =
    currentChapter?.lectures.filter((l) => l.completed).length || 0;
  const chapterProgressPercent =
    totalLecturesInChapter > 0
      ? Math.round((completedLecturesInChapter / totalLecturesInChapter) * 100)
      : 0;

  // Global total completed
  let totalAllLectures = 0;
  let totalAllCompleted = 0;
  sessions.forEach((s) => {
    s.subjects.forEach((sub) => {
      sub.chapters.forEach((c) => {
        c.lectures.forEach((l) => {
          totalAllLectures++;
          if (l.completed) totalAllCompleted++;
        });
      });
    });
  });

  // Check if current session is active now
  const isCurrentActive = currentSession
    ? isSessionActiveNow(currentSession.startTime, currentSession.endTime)
    : false;

  // Lecture toggling handler
  const handleToggleLectureComplete = (lectureId: string) => {
    if (!currentSession || !currentSubject || !currentChapter) return;

    let justCompleted = false;

    setSessions((prevSessions) =>
      prevSessions.map((session) => {
        if (session.id !== currentSession.id) return session;
        return {
          ...session,
          subjects: session.subjects.map((sub) => {
            if (sub.id !== currentSubject.id) return sub;
            return {
              ...sub,
              chapters: sub.chapters.map((chap) => {
                if (chap.id !== currentChapter.id) return chap;
                return {
                  ...chap,
                  lectures: chap.lectures.map((lec) => {
                    if (lec.id === lectureId) {
                      const newStatus = !lec.completed;
                      if (newStatus) justCompleted = true;
                      return {
                        ...lec,
                        completed: newStatus,
                        completedAt: newStatus ? new Date().toISOString() : undefined,
                      };
                    }
                    return lec;
                  }),
                };
              }),
            };
          }),
        };
      })
    );

    if (justCompleted) {
      soundManager.playSuccessChime();
      confetti({
        particleCount: 45,
        spread: 60,
        origin: { y: 0.8 },
      });
    }
  };

  // Bulk Link Importer Apply
  const handleApplyBulkLinks = (links: string[], autoExpand: boolean) => {
    if (!currentSession || !currentSubject || !currentChapter) return;

    setSessions((prevSessions) =>
      prevSessions.map((session) => {
        if (session.id !== currentSession.id) return session;
        return {
          ...session,
          subjects: session.subjects.map((sub) => {
            if (sub.id !== currentSubject.id) return sub;
            return {
              ...sub,
              chapters: sub.chapters.map((chap) => {
                if (chap.id !== currentChapter.id) return chap;

                const targetLength =
                  autoExpand && links.length > chap.lectures.length
                    ? links.length
                    : chap.lectures.length;

                const updatedLectures: Lecture[] = [];

                for (let i = 0; i < targetLength; i++) {
                  const existing = chap.lectures[i];
                  const newUrl = links[i] !== undefined ? links[i] : existing?.videoUrl || '';
                  if (existing) {
                    updatedLectures.push({
                      ...existing,
                      videoUrl: newUrl,
                    });
                  } else {
                    updatedLectures.push({
                      id: `lec-${i + 1}-${Math.random().toString(36).substring(2, 7)}`,
                      number: i + 1,
                      title: `লেকচার ${i + 1}`,
                      videoUrl: newUrl,
                      completed: false,
                      notes: '',
                    });
                  }
                }

                return {
                  ...chap,
                  totalLectures: targetLength,
                  lectures: updatedLectures,
                };
              }),
            };
          }),
        };
      })
    );

    soundManager.playSuccessChime();
  };

  // Total Lectures input adjuster
  const handleUpdateTotalLectures = (newCount: number) => {
    if (!currentSession || !currentSubject || !currentChapter || newCount <= 0) return;

    setSessions((prevSessions) =>
      prevSessions.map((session) => {
        if (session.id !== currentSession.id) return session;
        return {
          ...session,
          subjects: session.subjects.map((sub) => {
            if (sub.id !== currentSubject.id) return sub;
            return {
              ...sub,
              chapters: sub.chapters.map((chap) => {
                if (chap.id !== currentChapter.id) return chap;

                const currentList = [...chap.lectures];
                if (newCount > currentList.length) {
                  for (let i = currentList.length; i < newCount; i++) {
                    currentList.push({
                      id: `lec-${i + 1}-${Math.random().toString(36).substring(2, 7)}`,
                      number: i + 1,
                      title: `লেকচার ${i + 1}`,
                      videoUrl: '',
                      completed: false,
                      notes: '',
                    });
                  }
                } else if (newCount < currentList.length) {
                  currentList.splice(newCount);
                }

                return {
                  ...chap,
                  totalLectures: newCount,
                  lectures: currentList,
                };
              }),
            };
          }),
        };
      })
    );
  };

  // Save Chapter Handler
  const handleSaveChapter = (data: { name: string; chapterNumber: string; totalLectures: number }) => {
    if (!currentSession || !currentSubject) return;

    if (editingChapter) {
      // Edit existing chapter
      setSessions((prev) =>
        prev.map((s) => {
          if (s.id !== currentSession.id) return s;
          return {
            ...s,
            subjects: s.subjects.map((sub) => {
              if (sub.id !== currentSubject.id) return sub;
              return {
                ...sub,
                chapters: sub.chapters.map((c) => {
                  if (c.id !== editingChapter.id) return c;
                  // Adjust lectures length if total changed
                  const currentList = [...c.lectures];
                  if (data.totalLectures > currentList.length) {
                    for (let i = currentList.length; i < data.totalLectures; i++) {
                      currentList.push({
                        id: `lec-${i + 1}-${Math.random().toString(36).substring(2, 7)}`,
                        number: i + 1,
                        title: `লেকচার ${i + 1}`,
                        videoUrl: '',
                        completed: false,
                        notes: '',
                      });
                    }
                  } else if (data.totalLectures < currentList.length) {
                    currentList.splice(data.totalLectures);
                  }
                  return {
                    ...c,
                    name: data.name,
                    chapterNumber: data.chapterNumber,
                    totalLectures: data.totalLectures,
                    lectures: currentList,
                  };
                }),
              };
            }),
          };
        })
      );
    } else {
      // Create new chapter
      const newChapId = `chap-${Date.now()}`;
      const newLectures = Array.from({ length: data.totalLectures }, (_, i) => ({
        id: `lec-${i + 1}-${Math.random().toString(36).substring(2, 7)}`,
        number: i + 1,
        title: `লেকচার ${i + 1}`,
        videoUrl: '',
        completed: false,
        notes: '',
      }));

      const newChap: Chapter = {
        id: newChapId,
        name: data.name,
        chapterNumber: data.chapterNumber,
        totalLectures: data.totalLectures,
        lectures: newLectures,
        notes: '',
        createdAt: new Date().toISOString(),
      };

      setSessions((prev) =>
        prev.map((s) => {
          if (s.id !== currentSession.id) return s;
          return {
            ...s,
            subjects: s.subjects.map((sub) => {
              if (sub.id !== currentSubject.id) return sub;
              return {
                ...sub,
                chapters: [...sub.chapters, newChap],
              };
            }),
          };
        })
      );
      setSelectedChapterId(newChapId);
    }
    setShowChapterEditor(false);
    setEditingChapter(null);
  };

  // Delete Chapter Handler
  const handleDeleteChapter = (chapId: string) => {
    if (!currentSession || !currentSubject) return;
    setSessions((prev) =>
      prev.map((s) => {
        if (s.id !== currentSession.id) return s;
        return {
          ...s,
          subjects: s.subjects.map((sub) => {
            if (sub.id !== currentSubject.id) return sub;
            const filtered = sub.chapters.filter((c) => c.id !== chapId);
            return {
              ...sub,
              chapters: filtered,
            };
          }),
        };
      })
    );
    setShowChapterEditor(false);
    setEditingChapter(null);
  };

  // Save Subject Handler
  const handleSaveSubject = (data: { name: string; code: string; timeSlot: string; color: string }) => {
    if (!currentSession) return;

    if (editingSubject) {
      // Edit
      setSessions((prev) =>
        prev.map((s) => {
          if (s.id !== currentSession.id) return s;
          return {
            ...s,
            subjects: s.subjects.map((sub) => {
              if (sub.id !== editingSubject.id) return sub;
              return {
                ...sub,
                name: data.name,
                code: data.code,
                timeSlot: data.timeSlot,
                color: data.color,
              };
            }),
          };
        })
      );
    } else {
      // Add
      const newSubId = `sub-${Date.now()}`;
      const defaultNewChap: Chapter = {
        id: `chap-${Date.now()}`,
        name: 'অধ্যায় ০১: পরিচিতি',
        chapterNumber: '০১',
        totalLectures: 10,
        lectures: Array.from({ length: 10 }, (_, i) => ({
          id: `lec-${i + 1}-${Math.random().toString(36).substring(2, 7)}`,
          number: i + 1,
          title: `লেকচার ${i + 1}`,
          videoUrl: '',
          completed: false,
          notes: '',
        })),
        notes: '',
        createdAt: new Date().toISOString(),
      };

      const newSubject: Subject = {
        id: newSubId,
        name: data.name,
        code: data.code,
        timeSlot: data.timeSlot,
        color: data.color,
        chapters: [defaultNewChap],
      };

      setSessions((prev) =>
        prev.map((s) => {
          if (s.id !== currentSession.id) return s;
          return {
            ...s,
            subjects: [...s.subjects, newSubject],
          };
        })
      );
      setSelectedSubjectId(newSubId);
      setSelectedChapterId(defaultNewChap.id);
    }
    setShowSubjectEditor(false);
    setEditingSubject(null);
  };

  // Delete Subject Handler
  const handleDeleteSubject = (subjectId: string) => {
    if (!currentSession) return;
    setSessions((prev) =>
      prev.map((s) => {
        if (s.id !== currentSession.id) return s;
        return {
          ...s,
          subjects: s.subjects.filter((sub) => sub.id !== subjectId),
        };
      })
    );
    setShowSubjectEditor(false);
    setEditingSubject(null);
  };

  // Save Session Handler
  const handleSaveSession = (data: { name: string; startTime: string; endTime: string; color: string }) => {
    if (editingSession) {
      setSessions((prev) =>
        prev.map((s) => {
          if (s.id !== editingSession.id) return s;
          return {
            ...s,
            name: data.name,
            startTime: data.startTime,
            endTime: data.endTime,
            color: data.color,
          };
        })
      );
    } else {
      const newSessionId = `session-${Date.now()}`;
      const newSession: StudySession = {
        id: newSessionId,
        name: data.name,
        startTime: data.startTime,
        endTime: data.endTime,
        color: data.color,
        subjects: [
          {
            id: `sub-${Date.now()}`,
            name: 'নতুন বিষয়',
            code: 'SUB-101',
            timeSlot: `${data.startTime} - ${data.endTime}`,
            color: data.color,
            chapters: [
              {
                id: `chap-${Date.now()}`,
                name: 'অধ্যায় ০১: প্রাথমিক আলোচনা',
                chapterNumber: '০১',
                totalLectures: 10,
                lectures: Array.from({ length: 10 }, (_, i) => ({
                  id: `lec-${i + 1}-${Math.random().toString(36).substring(2, 7)}`,
                  number: i + 1,
                  title: `লেকচার ${i + 1}`,
                  videoUrl: '',
                  completed: false,
                  notes: '',
                })),
                notes: '',
                createdAt: new Date().toISOString(),
              },
            ],
          },
        ],
      };
      setSessions((prev) => [...prev, newSession]);
      setSelectedSessionId(newSessionId);
    }
    setShowSessionEditor(false);
    setEditingSession(null);
  };

  // Delete Session Handler
  const handleDeleteSession = (sessionId: string) => {
    if (sessions.length <= 1) {
      alert('কমপক্ষে একটি সেশন থাকা আবশ্যক!');
      return;
    }
    setSessions((prev) => prev.filter((s) => s.id !== sessionId));
    setSelectedSessionId(sessions.find((s) => s.id !== sessionId)?.id || '');
    setShowSessionEditor(false);
    setEditingSession(null);
  };

  // Filter and search lectures
  const displayedLectures = (currentChapter?.lectures || []).filter((lec) => {
    if (filterMode === 'pending' && lec.completed) return false;
    if (filterMode === 'completed' && !lec.completed) return false;
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      const matchesNum = lec.number.toString().includes(query);
      const matchesTitle = lec.title.toLowerCase().includes(query);
      const matchesNotes = (lec.notes || '').toLowerCase().includes(query);
      return matchesNum || matchesTitle || matchesNotes;
    }
    return true;
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-['Hind_Siliguri',sans-serif]">
      {/* Top Main Navigation Header */}
      <header className="sticky top-0 z-40 bg-slate-950/90 backdrop-blur-md border-b border-slate-800/80 px-4 lg:px-8 py-3.5">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3">
          {/* Logo & Title */}
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-emerald-600 via-blue-600 to-indigo-600 p-0.5 shadow-lg shadow-emerald-950/40">
              <div className="w-full h-full bg-slate-950 rounded-[14px] flex items-center justify-center text-emerald-400">
                <BookOpen className="w-5 h-5" />
              </div>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base sm:text-lg font-bold text-white tracking-tight">
                  স্টাডি রিমাইন্ডার ও লেকচার ট্র্যাকার
                </h1>
                <span className="hidden sm:inline-block px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 text-[11px] font-semibold border border-emerald-500/20">
                  v2.0 Pro
                </span>
              </div>
              <p className="text-xs text-slate-400">
                সেশন ম্যানেজমেন্ট • স্মার্ট লিঙ্ক ম্যাপিং • ইউটিউব প্লেয়ার • প্রোগ্রেস ট্র্যাকার
              </p>
            </div>
          </div>

          {/* Current Clock & Quick Stats */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Real-time Digital Clock & Active Session indicator */}
            <div className="hidden md:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 text-xs">
              <Clock className="w-3.5 h-3.5 text-blue-400 animate-spin-slow" />
              <span className="font-mono text-slate-200 font-bold">
                {currentTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
              </span>
              {isCurrentActive && (
                <span className="flex items-center gap-1 text-[11px] text-emerald-400 font-bold bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20 animate-pulse">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                  সেশন চলমান
                </span>
              )}
            </div>

            {/* Android Java+XML Source Code (GitHub Ready) Button */}
            <button
              onClick={() => setShowAndroidSource(true)}
              className="px-3 py-1.5 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer shadow-sm hover:scale-[1.02]"
              title="Android Java + XML সোর্স কোড দেখুন এবং GitHub এ আপলোড করুন"
            >
              <Smartphone className="w-3.5 h-3.5 text-emerald-400" />
              <span>Android কোড (GitHub)</span>
            </button>

            {/* Cloud & Drive Backup */}
            <button
              onClick={() => setShowDriveSync(true)}
              className="px-3 py-1.5 rounded-xl bg-blue-500/10 hover:bg-blue-500/20 text-blue-300 border border-blue-500/30 text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer"
              title="Google Drive ব্যাকআপ এবং ক্লাউড সিঙ্ক"
            >
              <Cloud className="w-3.5 h-3.5 text-blue-400" />
              <span className="hidden sm:inline">ব্যাকআপ ও সিঙ্ক</span>
            </button>

            {/* Add New Session Button */}
            <button
              onClick={() => {
                setEditingSession(null);
                setShowSessionEditor(true);
              }}
              className="px-3.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold flex items-center gap-1.5 shadow-lg shadow-blue-600/25 transition-all cursor-pointer hover:scale-[1.02]"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Add New Session</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 lg:p-6 grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* LEFT COLUMN: SESSIONS & POMODORO (lg:col-span-4) */}
        <aside className="lg:col-span-4 space-y-5">
          {/* SESSIONS MANAGEMENT CARD */}
          <div className="bg-slate-900/90 border border-slate-800/80 rounded-2xl p-4 md:p-5 shadow-xl">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
                  <Clock className="w-4 h-4" />
                </div>
                <div>
                  <h2 className="text-sm font-bold text-white">স্টাডি সেশনসমূহ (Sessions)</h2>
                  <p className="text-[11px] text-slate-400">ডিফল্ট ৩টি সেশন ও কাস্টম সূচি</p>
                </div>
              </div>
              <button
                onClick={() => {
                  setEditingSession(null);
                  setShowSessionEditor(true);
                }}
                className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                title="নতুন সেশন যোগ করুন"
              >
                <Plus className="w-4 h-4" />
              </button>
            </div>

            {/* Sessions List */}
            <div className="space-y-2.5">
              {sessions.map((session, idx) => {
                const isSelected = session.id === selectedSessionId;
                const isActiveNow = isSessionActiveNow(session.startTime, session.endTime);

                // calculate session completion stats
                let sessionTotal = 0;
                let sessionDone = 0;
                session.subjects.forEach((sub) =>
                  sub.chapters.forEach((c) =>
                    c.lectures.forEach((l) => {
                      sessionTotal++;
                      if (l.completed) sessionDone++;
                    })
                  )
                );

                return (
                  <div
                    key={session.id}
                    onClick={() => {
                      setSelectedSessionId(session.id);
                      if (session.subjects.length > 0) {
                        setSelectedSubjectId(session.subjects[0].id);
                        if (session.subjects[0].chapters.length > 0) {
                          setSelectedChapterId(session.subjects[0].chapters[0].id);
                        }
                      }
                    }}
                    className={`relative p-3.5 rounded-xl border transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-slate-800/90 border-blue-500/60 shadow-lg shadow-blue-950/50 ring-1 ring-blue-500/30'
                        : 'bg-slate-950/60 hover:bg-slate-800/40 border-slate-800/80'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2.5">
                        <div
                          className="w-2.5 h-9 rounded-full"
                          style={{ backgroundColor: session.color || '#3b82f6' }}
                        />
                        <div>
                          <div className="flex items-center gap-2">
                            <h3 className="text-xs font-bold text-white">{session.name}</h3>
                            {isActiveNow && (
                              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
                            )}
                          </div>
                          <p className="text-[11px] text-slate-400 mt-0.5">
                            {session.subjects.length}টি বিষয় • {sessionDone}/{sessionTotal} ক্লাস সম্পন্ন
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-1">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setEditingSession(session);
                            setShowSessionEditor(true);
                          }}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700/60 transition-colors"
                          title="সেশন সম্পাদনা"
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>

                    {/* Subject Pills Inside Session */}
                    <div className="flex flex-wrap gap-1.5 mt-2.5 pl-5">
                      {session.subjects.map((sub) => (
                        <span
                          key={sub.id}
                          className="text-[10px] px-2 py-0.5 rounded-md bg-slate-900 border border-slate-700/80 text-slate-300 font-medium"
                        >
                          {sub.name.split(' ')[0]} • {sub.timeSlot}
                        </span>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* FOCUS STUDY TIMER */}
          <PomodoroTimer />

          {/* OVERALL STUDY STATS CARD */}
          <div className="bg-slate-900/70 border border-slate-800/80 rounded-2xl p-4 text-xs">
            <div className="flex items-center justify-between mb-2">
              <span className="font-bold text-white flex items-center gap-1.5">
                <Award className="w-4 h-4 text-amber-400" />
                সামগ্রিক প্রোগ্রেস (Overall Stats)
              </span>
              <span className="text-emerald-400 font-bold">
                {totalAllLectures > 0 ? Math.round((totalAllCompleted / totalAllLectures) * 100) : 0}%
              </span>
            </div>
            <div className="w-full bg-slate-950 h-2 rounded-full overflow-hidden border border-slate-800">
              <div
                className="h-full bg-gradient-to-r from-blue-500 to-emerald-500 transition-all duration-500"
                style={{
                  width: `${totalAllLectures > 0 ? (totalAllCompleted / totalAllLectures) * 100 : 0}%`,
                }}
              />
            </div>
            <div className="flex items-center justify-between text-[11px] text-slate-400 mt-2">
              <span>মোট ক্লাস: {totalAllLectures}টি</span>
              <span className="text-emerald-400 font-semibold">সম্পন্ন: {totalAllCompleted}টি</span>
            </div>
          </div>
        </aside>

        {/* RIGHT/CENTER COLUMN: SUBJECT & CHAPTER LECTURE WORKSPACE (lg:col-span-8) */}
        <section className="lg:col-span-8 space-y-5">
          {/* SUBJECT SELECTION BAR */}
          <div className="bg-slate-900/90 border border-slate-800/80 rounded-2xl p-4 shadow-xl">
            <div className="flex flex-wrap items-center justify-between gap-3 mb-3">
              <div className="flex items-center gap-2">
                <Book className="w-4 h-4 text-emerald-400" />
                <span className="text-xs font-bold text-slate-200 uppercase tracking-wider">
                  বিষয় নির্বাচন (Subjects in {currentSession?.name.split(':')[0]})
                </span>
              </div>
              <button
                onClick={() => {
                  setEditingSubject(null);
                  setShowSubjectEditor(true);
                }}
                className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-emerald-400 text-xs font-semibold flex items-center gap-1 border border-slate-700 transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Subject</span>
              </button>
            </div>

            {/* Subject Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2.5">
              {currentSession?.subjects.map((sub) => {
                const isSubSelected = sub.id === selectedSubjectId;
                return (
                  <div
                    key={sub.id}
                    onClick={() => {
                      setSelectedSubjectId(sub.id);
                      if (sub.chapters.length > 0) {
                        setSelectedChapterId(sub.chapters[0].id);
                      }
                    }}
                    className={`p-3 rounded-xl border transition-all cursor-pointer flex flex-col justify-between ${
                      isSubSelected
                        ? 'bg-slate-800/95 border-emerald-500/60 shadow-md ring-1 ring-emerald-500/30'
                        : 'bg-slate-950/60 hover:bg-slate-800/40 border-slate-800/80'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-1">
                      <h4 className="text-xs font-bold text-white line-clamp-1">{sub.name}</h4>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setEditingSubject(sub);
                          setShowSubjectEditor(true);
                        }}
                        className="p-1 text-slate-400 hover:text-white rounded"
                        title="বিষয় ও সময় পরিবর্তন"
                      >
                        <Edit2 className="w-3 h-3" />
                      </button>
                    </div>
                    <div className="flex items-center justify-between text-[11px] text-slate-400 mt-2">
                      <span className="text-emerald-400 font-mono font-medium">{sub.timeSlot}</span>
                      <span>{sub.chapters.length} অধ্যায়</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* CHAPTER & LECTURE CONTROLLER CARD */}
          {currentSubject && (
            <div className="bg-slate-900/90 border border-slate-800/80 rounded-2xl p-4 md:p-6 shadow-xl space-y-5">
              {/* Chapter Header & Selector */}
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800/80 pb-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-purple-500/10 border border-purple-500/30 flex items-center justify-center text-purple-400">
                    <Layers className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-semibold px-2 py-0.5 rounded bg-blue-500/20 text-blue-300 border border-blue-500/30">
                        {currentSubject.name}
                      </span>
                      <span className="text-xs text-slate-400">
                        সূচি: <strong className="text-emerald-400">{currentSubject.timeSlot}</strong>
                      </span>
                    </div>
                    <h2 className="text-lg font-bold text-white mt-1">
                      {currentChapter ? currentChapter.name : 'কোনো অধ্যায় নেই'}
                    </h2>
                  </div>
                </div>

                {/* Chapter Actions */}
                <div className="flex flex-wrap items-center gap-2">
                  {/* Chapter Switcher Dropdown */}
                  {currentSubject.chapters.length > 1 && (
                    <select
                      value={selectedChapterId}
                      onChange={(e) => setSelectedChapterId(e.target.value)}
                      className="bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-purple-500"
                    >
                      {currentSubject.chapters.map((chap) => (
                        <option key={chap.id} value={chap.id}>
                          {chap.name}
                        </option>
                      ))}
                    </select>
                  )}

                  {/* Add Chapter Button */}
                  <button
                    onClick={() => {
                      setEditingChapter(null);
                      setShowChapterEditor(true);
                    }}
                    className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-purple-300 border border-purple-500/30 text-xs font-semibold flex items-center gap-1 transition-colors cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>নতুন অধ্যায়</span>
                  </button>

                  {/* Edit Chapter Button */}
                  {currentChapter && (
                    <button
                      onClick={() => {
                        setEditingChapter(currentChapter);
                        setShowChapterEditor(true);
                      }}
                      className="p-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                      title="অধ্যায়ের নাম ও মোট লেকচার সম্পাদনা"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>

              {/* OVERALL PROGRESS BAR & CONTROL PANEL */}
              {currentChapter && (
                <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4 space-y-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <CheckCircle className="w-4 h-4 text-emerald-400" />
                      <span className="text-xs font-bold text-white">
                        অধ্যায় প্রোগ্রেস ট্র্যাকিং:
                      </span>
                      <span className="text-xs font-mono font-bold text-emerald-400">
                        {completedLecturesInChapter} / {totalLecturesInChapter} সম্পন্ন ({chapterProgressPercent}%)
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      {/* Total Lectures Quick Adjuster */}
                      <div className="flex items-center gap-1.5 bg-slate-900 px-2.5 py-1 rounded-lg border border-slate-800 text-xs">
                        <span className="text-slate-400 font-medium">Total Lectures:</span>
                        <input
                          type="number"
                          min={1}
                          max={150}
                          value={currentChapter.totalLectures}
                          onChange={(e) => handleUpdateTotalLectures(parseInt(e.target.value, 10) || 1)}
                          className="w-12 bg-slate-950 border border-slate-700 rounded px-1.5 py-0.5 text-center font-bold text-purple-300 focus:outline-none focus:border-purple-500"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Visual Progress Bar */}
                  <div className="w-full bg-slate-900 h-3 rounded-full overflow-hidden border border-slate-800/80 p-0.5">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-emerald-600 to-emerald-400 transition-all duration-500 shadow-sm shadow-emerald-500/50"
                      style={{ width: `${chapterProgressPercent}%` }}
                    />
                  </div>

                  {/* Quick Action Buttons: Bulk Link Importer & Notes */}
                  <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-800/80">
                    <div className="flex flex-wrap items-center gap-2">
                      {/* SMART LINK MAPPING BUTTON (Bulk Link Importer) */}
                      <button
                        onClick={() => setShowBulkImporter(true)}
                        className="px-3.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold flex items-center gap-1.5 shadow-lg shadow-blue-600/20 transition-all cursor-pointer"
                      >
                        <Link2 className="w-3.5 h-3.5" />
                        <span>স্মার্ট লিঙ্ক ম্যাপিং (Bulk Link Importer)</span>
                      </button>

                      {/* CHAPTER NOTES BUTTON */}
                      <button
                        onClick={() => {
                          setQuickNotesTarget({
                            title: currentSubject.name,
                            subtitle: currentChapter.name,
                            notes: currentChapter.notes || '',
                            onSave: (newNotes) => {
                              setSessions((prev) =>
                                prev.map((s) => {
                                  if (s.id !== currentSession.id) return s;
                                  return {
                                    ...s,
                                    subjects: s.subjects.map((sub) => {
                                      if (sub.id !== currentSubject.id) return sub;
                                      return {
                                        ...sub,
                                        chapters: sub.chapters.map((c) => {
                                          if (c.id !== currentChapter.id) return c;
                                          return { ...c, notes: newNotes };
                                        }),
                                      };
                                    }),
                                  };
                                })
                              );
                            },
                          });
                        }}
                        className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-amber-300 border border-amber-500/30 text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
                      >
                        <BookOpen className="w-3.5 h-3.5 text-amber-400" />
                        <span>অধ্যায় নোটস ও সূত্র {currentChapter.notes ? '📝' : ''}</span>
                      </button>
                    </div>

                    {/* Filter Mode Pills */}
                    <div className="flex items-center gap-1 bg-slate-900 p-1 rounded-lg border border-slate-800 text-xs">
                      <button
                        onClick={() => setFilterMode('all')}
                        className={`px-2 py-0.5 rounded text-[11px] font-medium transition-colors ${
                          filterMode === 'all' ? 'bg-slate-800 text-white' : 'text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        সব ({currentChapter.lectures.length})
                      </button>
                      <button
                        onClick={() => setFilterMode('pending')}
                        className={`px-2 py-0.5 rounded text-[11px] font-medium transition-colors ${
                          filterMode === 'pending' ? 'bg-amber-500/20 text-amber-300' : 'text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        বাকি ({currentChapter.lectures.filter((l) => !l.completed).length})
                      </button>
                      <button
                        onClick={() => setFilterMode('completed')}
                        className={`px-2 py-0.5 rounded text-[11px] font-medium transition-colors ${
                          filterMode === 'completed' ? 'bg-emerald-500/20 text-emerald-300' : 'text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        সম্পন্ন ({completedLecturesInChapter})
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* LECTURES GRID (Lecture 1, Lecture 2, ... Lecture N) */}
              {currentChapter && (
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-2">
                      <span>লেকচারসমূহ (১-ক্লিকে প্লে ও ট্র্যাকিং)</span>
                      <span className="text-[11px] text-slate-500 font-normal">
                        ({displayedLectures.length}টি প্রদর্শিত)
                      </span>
                    </h3>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                    {displayedLectures.map((lecture) => {
                      const hasVideo = Boolean(lecture.videoUrl && lecture.videoUrl.trim());

                      return (
                        <div
                          key={lecture.id}
                          className={`group relative rounded-2xl p-3.5 border transition-all duration-200 flex flex-col justify-between ${
                            lecture.completed
                              ? 'bg-emerald-950/40 hover:bg-emerald-950/60 border-emerald-500/40 shadow-sm shadow-emerald-950/30'
                              : 'bg-slate-950/70 hover:bg-slate-850 border-slate-800/80 hover:border-slate-700'
                          }`}
                        >
                          {/* Top Row: Title & Completion Checkbox */}
                          <div className="flex items-start justify-between gap-2">
                            <div className="flex items-center gap-2">
                              <span
                                className={`w-6 h-6 rounded-lg text-xs font-bold flex items-center justify-center font-mono ${
                                  lecture.completed
                                    ? 'bg-emerald-500 text-white'
                                    : 'bg-slate-800 text-slate-300 border border-slate-700'
                                }`}
                              >
                                {lecture.number}
                              </span>
                              <h4
                                className={`text-xs font-bold transition-colors ${
                                  lecture.completed ? 'text-emerald-200 line-through opacity-90' : 'text-white'
                                }`}
                              >
                                {lecture.title}
                              </h4>
                            </div>

                            {/* Completion Checkbox */}
                            <button
                              type="button"
                              onClick={() => handleToggleLectureComplete(lecture.id)}
                              className={`p-1 rounded-lg transition-transform hover:scale-110 cursor-pointer ${
                                lecture.completed ? 'text-emerald-400' : 'text-slate-500 hover:text-slate-300'
                              }`}
                              title={lecture.completed ? 'সম্পন্ন হিসেবে চিহ্নিত' : 'সম্পন্ন চিহ্নিত করুন'}
                            >
                              {lecture.completed ? (
                                <CheckSquare className="w-5 h-5 text-emerald-400 fill-emerald-500/20" />
                              ) : (
                                <Square className="w-5 h-5" />
                              )}
                            </button>
                          </div>

                          {/* Middle: Video Status or URL snippet */}
                          <div className="my-2.5">
                            {hasVideo ? (
                              <div className="flex items-center gap-1.5 text-[11px] text-slate-400">
                                <span className="w-2 h-2 rounded-full bg-red-500" />
                                <span className="truncate max-w-[150px] font-mono">
                                  {lecture.videoUrl}
                                </span>
                              </div>
                            ) : (
                              <div className="text-[11px] text-slate-500 flex items-center gap-1">
                                <span>ভিডিও লিঙ্ক নেই</span>
                              </div>
                            )}
                          </div>

                          {/* Bottom Row: 1-Click Play Button & Notes */}
                          <div className="flex items-center justify-between gap-1.5 pt-2 border-t border-slate-800/60">
                            {/* 1-Click Play Button */}
                            <button
                              type="button"
                              onClick={() => {
                                if (hasVideo) {
                                  setActiveVideoLecture(lecture);
                                } else {
                                  setShowBulkImporter(true);
                                }
                              }}
                              className={`flex-1 py-1.5 px-2.5 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                                hasVideo
                                  ? lecture.completed
                                    ? 'bg-emerald-600/30 hover:bg-emerald-600/50 text-emerald-200 border border-emerald-500/40'
                                    : 'bg-red-600/20 hover:bg-red-600 text-red-300 hover:text-white border border-red-500/30 shadow-md shadow-red-900/20'
                                  : 'bg-slate-800 hover:bg-slate-700 text-slate-400 border border-slate-700'
                              }`}
                            >
                              <Play className="w-3.5 h-3.5 fill-current" />
                              <span>{hasVideo ? '১-ক্লিকে প্লে' : 'লিঙ্ক দিন'}</span>
                            </button>

                            {/* Lecture Notes Button */}
                            <button
                              type="button"
                              onClick={() => {
                                setQuickNotesTarget({
                                  title: lecture.title,
                                  subtitle: `${currentSubject.name} • ${currentChapter.name}`,
                                  notes: lecture.notes || '',
                                  onSave: (newNotes) => {
                                    setSessions((prev) =>
                                      prev.map((s) => {
                                        if (s.id !== currentSession.id) return s;
                                        return {
                                          ...s,
                                          subjects: s.subjects.map((sub) => {
                                            if (sub.id !== currentSubject.id) return sub;
                                            return {
                                              ...sub,
                                              chapters: sub.chapters.map((c) => {
                                                if (c.id !== currentChapter.id) return c;
                                                return {
                                                  ...c,
                                                  lectures: c.lectures.map((l) =>
                                                    l.id === lecture.id ? { ...l, notes: newNotes } : l
                                                  ),
                                                };
                                              }),
                                            };
                                          }),
                                        };
                                      })
                                    );
                                  },
                                });
                              }}
                              className={`p-1.5 rounded-xl border transition-colors ${
                                lecture.notes
                                  ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                                  : 'bg-slate-800 hover:bg-slate-700 text-slate-400 border-slate-700'
                              }`}
                              title="লেকচার নোটস"
                            >
                              <BookOpen className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}
        </section>
      </main>

      {/* FOOTER */}
      <footer className="mt-12 border-t border-slate-800/80 bg-slate-950/80 py-6 px-4 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          <p>© 2026 স্টাডি রিমাইন্ডার ও লেকচার ট্র্যাকার • সমস্ত ডেটা লোকাল স্টোরেজ ও ড্রাইভে সুরক্ষিত।</p>
          <div className="flex items-center gap-3">
            <button
              onClick={() => setShowAndroidSource(true)}
              className="text-emerald-400 hover:underline cursor-pointer flex items-center gap-1"
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Android Java+XML GitHub সোর্স</span>
            </button>
            <span>•</span>
            <button
              onClick={() => setShowDriveSync(true)}
              className="text-blue-400 hover:underline cursor-pointer flex items-center gap-1"
            >
              <Cloud className="w-3.5 h-3.5" />
              <span>ক্লাউড ব্যাকআপ</span>
            </button>
          </div>
        </div>
      </footer>

      {/* MODALS */}
      {/* 1. Video Player Modal */}
      {activeVideoLecture && currentChapter && currentSubject && (
        <VideoPlayerModal
          lecture={activeVideoLecture}
          chapterName={currentChapter.name}
          subjectName={currentSubject.name}
          onClose={() => setActiveVideoLecture(null)}
          onToggleComplete={(lecId) => {
            handleToggleLectureComplete(lecId);
            setActiveVideoLecture((prev) => (prev ? { ...prev, completed: !prev.completed } : null));
          }}
          onOpenNotes={(lec) => {
            setQuickNotesTarget({
              title: lec.title,
              subtitle: `${currentSubject.name} • ${currentChapter.name}`,
              notes: lec.notes || '',
              onSave: (newNotes) => {
                setSessions((prev) =>
                  prev.map((s) => {
                    if (s.id !== currentSession.id) return s;
                    return {
                      ...s,
                      subjects: s.subjects.map((sub) => {
                        if (sub.id !== currentSubject.id) return sub;
                        return {
                          ...sub,
                          chapters: sub.chapters.map((c) => {
                            if (c.id !== currentChapter.id) return c;
                            return {
                              ...c,
                              lectures: c.lectures.map((l) =>
                                l.id === lec.id ? { ...l, notes: newNotes } : l
                              ),
                            };
                          }),
                        };
                      }),
                    };
                  })
                );
              },
            });
          }}
        />
      )}

      {/* 2. Bulk Link Importer Modal */}
      {showBulkImporter && currentChapter && (
        <BulkLinkImporterModal
          chapterName={currentChapter.name}
          totalLectures={currentChapter.totalLectures}
          onApplyLinks={handleApplyBulkLinks}
          onClose={() => setShowBulkImporter(false)}
        />
      )}

      {/* 3. Session Editor Modal */}
      {showSessionEditor && (
        <SessionEditorModal
          session={editingSession}
          onSave={handleSaveSession}
          onDelete={handleDeleteSession}
          onClose={() => {
            setShowSessionEditor(false);
            setEditingSession(null);
          }}
        />
      )}

      {/* 4. Subject Editor Modal */}
      {showSubjectEditor && currentSession && (
        <SubjectEditorModal
          subject={editingSubject}
          sessionName={currentSession.name}
          onSave={handleSaveSubject}
          onDelete={handleDeleteSubject}
          onClose={() => {
            setShowSubjectEditor(false);
            setEditingSubject(null);
          }}
        />
      )}

      {/* 5. Chapter Editor Modal */}
      {showChapterEditor && currentSubject && (
        <ChapterEditorModal
          chapter={editingChapter}
          subjectName={currentSubject.name}
          onSave={handleSaveChapter}
          onDelete={handleDeleteChapter}
          onClose={() => {
            setShowChapterEditor(false);
            setEditingChapter(null);
          }}
        />
      )}

      {/* 6. Quick Notes Modal */}
      {quickNotesTarget && (
        <QuickNotesModal
          title={quickNotesTarget.title}
          subtitle={quickNotesTarget.subtitle}
          initialNotes={quickNotesTarget.notes}
          onSave={quickNotesTarget.onSave}
          onClose={() => setQuickNotesTarget(null)}
        />
      )}

      {/* 7. Google Drive Sync & Backup Modal */}
      {showDriveSync && (
        <GoogleDriveSyncModal
          sessions={sessions}
          settings={settings}
          onRestoreData={(restoredSessions, restoredSettings) => {
            setSessions(restoredSessions);
            if (restoredSettings) setSettings(restoredSettings);
            if (restoredSessions.length > 0) {
              setSelectedSessionId(restoredSessions[0].id);
              if (restoredSessions[0].subjects.length > 0) {
                setSelectedSubjectId(restoredSessions[0].subjects[0].id);
                if (restoredSessions[0].subjects[0].chapters.length > 0) {
                  setSelectedChapterId(restoredSessions[0].subjects[0].chapters[0].id);
                }
              }
            }
          }}
          onResetDefaults={() => {
            setSessions(INITIAL_SESSIONS);
            setSettings(INITIAL_SETTINGS);
            setSelectedSessionId(INITIAL_SESSIONS[0].id);
            setSelectedSubjectId(INITIAL_SESSIONS[0].subjects[0].id);
            setSelectedChapterId(INITIAL_SESSIONS[0].subjects[0].chapters[0].id);
          }}
          onClose={() => setShowDriveSync(false)}
        />
      )}

      {/* 8. Android Native Java + XML Source Code Exporter Modal */}
      {showAndroidSource && (
        <AndroidSourceModal onClose={() => setShowAndroidSource(false)} />
      )}
    </div>
  );
}
