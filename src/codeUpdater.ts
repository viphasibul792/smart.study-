import { StudySession, Subject, Chapter, Lecture } from './types';

/**
 * কোড আপডেটার পার্সার
 * ----------------------
 * সহজ টেক্সট-কোড লিখে Session, Subject, Chapter, Lecture (YouTube ক্লাস)
 * যোগ বা আপডেট করা যায়। ফরম্যাট:
 *
 *   SESSION: সেশন ২: রাত ৯:০০ - ১০:০০ | 21:00 - 22:00
 *   SUBJECT: রসায়ন (Chemistry) | CHE-101
 *   CHAPTER: অধ্যায় ০১: মৌলের পর্যায়বৃত্ত ধর্ম | 15
 *   1. পর্যায় সারণির ইতিহাস: https://www.youtube.com/watch?v=XXXX
 *   2. ইলেকট্রন বিন্যাস: https://youtu.be/YYYY
 *
 * - SESSION/SUBJECT/CHAPTER এর বদলে সেশন:/বিষয়:/অধ্যায়: (বাংলা কীওয়ার্ড) ও চলবে
 * - নম্বর দেওয়া লেকচার আগে থেকে থাকলে সেটির টাইটেল/লিঙ্ক আপডেট হবে,
 *   না থাকলে নতুন লেকচার হিসেবে যোগ হবে
 * - CHAPTER না লিখলে লেকচারগুলো বর্তমানে খোলা অধ্যায়ে যোগ হবে
 * - JSON ব্যাকআপ পেস্ট করলেও কাজ করবে (সম্পূর্ণ ডেটা প্রতিস্থাপন)
 */

export interface ParsedLecture {
  number?: number;
  title: string;
  videoUrl: string;
}

export interface ParsedChapter {
  name: string;
  totalLectures?: number;
  lectures: ParsedLecture[];
}

export interface ParsedSubject {
  name: string;
  code?: string;
  timeSlot?: string;
  chapters: ParsedChapter[];
}

export interface ParsedSession {
  name: string;
  startTime?: string;
  endTime?: string;
  subjects: ParsedSubject[];
}

export interface ParseResult {
  mode: 'code' | 'json';
  sessions: ParsedSession[];
  /** CHAPTER ঘোষণা ছাড়া পাওয়া লেকচার — বর্তমান অধ্যায়ে যাবে */
  orphanLectures: ParsedLecture[];
  jsonSessions?: StudySession[];
  errors: string[];
  stats: { sessions: number; subjects: number; chapters: number; lectures: number };
}

const PALETTE = ['#3b82f6', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#06b6d4', '#ef4444', '#84cc16'];

const bnDigits: Record<string, string> = {
  '০': '0', '১': '1', '২': '2', '৩': '3', '৪': '4',
  '৫': '5', '৬': '6', '৭': '7', '৮': '8', '৯': '9',
};

function toEnDigits(s: string): string {
  return s.replace(/[০-৯]/g, (d) => bnDigits[d] || d);
}

function genId(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).substring(2, 8)}`;
}

const URL_RE = /(https?:\/\/[^\s]+)/i;
const TIME_RANGE_RE = /(\d{1,2}:\d{2})\s*[-–—]\s*(\d{1,2}:\d{2})/;

function parseLectureLine(line: string): ParsedLecture | null {
  const urlMatch = line.match(URL_RE);
  const numbered = toEnDigits(line).match(/^\s*(\d+)\s*[.)।]\s*(.*)$/);
  if (!urlMatch && !numbered) return null;

  let rest = line;
  let number: number | undefined;
  if (numbered) {
    number = parseInt(numbered[1], 10);
    // মূল লাইনে নম্বরের পরের অংশ বের করি (বাংলা ডিজিট থাকতে পারে)
    const m = line.match(/^\s*[\d০-৯]+\s*[.)।]\s*(.*)$/);
    rest = m ? m[1] : numbered[2];
  }

  let videoUrl = '';
  let title = rest.trim();
  if (urlMatch) {
    videoUrl = urlMatch[1].replace(/[),.;]+$/, '');
    title = rest.replace(URL_RE, '').replace(/[\s:|–—-]+$/, '').trim();
  }

  if (!title && number) title = `লেকচার ${number}`;
  if (!title && !videoUrl) return null;
  if (!title) title = 'লেকচার';

  return { number, title, videoUrl };
}

export function parseUpdateCode(text: string): ParseResult {
  const result: ParseResult = {
    mode: 'code',
    sessions: [],
    orphanLectures: [],
    errors: [],
    stats: { sessions: 0, subjects: 0, chapters: 0, lectures: 0 },
  };

  const trimmed = text.trim();
  if (!trimmed) {
    result.errors.push('কোনো কোড লেখা হয়নি।');
    return result;
  }

  // JSON মোড
  if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
    try {
      const parsed = JSON.parse(trimmed);
      const sessions: unknown = Array.isArray(parsed) ? parsed : parsed.sessions;
      if (Array.isArray(sessions) && sessions.every((s) => s && typeof s === 'object' && 'subjects' in s)) {
        result.mode = 'json';
        result.jsonSessions = sessions as StudySession[];
        result.stats.sessions = sessions.length;
        result.stats.subjects = sessions.reduce((a: number, s: StudySession) => a + (s.subjects?.length || 0), 0);
        result.stats.chapters = sessions.reduce(
          (a: number, s: StudySession) => a + s.subjects.reduce((b, su) => b + (su.chapters?.length || 0), 0), 0);
        result.stats.lectures = sessions.reduce(
          (a: number, s: StudySession) =>
            a + s.subjects.reduce((b, su) => b + su.chapters.reduce((c, ch) => c + (ch.lectures?.length || 0), 0), 0), 0);
        return result;
      }
      result.errors.push('JSON ফরম্যাট সঠিক নয় — "sessions" অ্যারে পাওয়া যায়নি।');
      return result;
    } catch (e) {
      result.errors.push('JSON পার্স করা যায়নি: ' + (e as Error).message);
      return result;
    }
  }

  // কোড মোড
  let curSession: ParsedSession | null = null;
  let curSubject: ParsedSubject | null = null;
  let curChapter: ParsedChapter | null = null;

  const lines = text.split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    const raw = lines[i];
    const line = raw.trim();
    if (!line || line.startsWith('#') || line.startsWith('//')) continue;

    const kw = line.match(/^(SESSION|সেশন|SUBJECT|বিষয়|CHAPTER|অধ্যায়)\s*[:：]\s*(.+)$/i);
    if (kw) {
      const keyword = kw[1].toUpperCase();
      const parts = kw[2].split('|').map((p) => p.trim()).filter(Boolean);
      const name = parts[0] || '';
      if (!name) {
        result.errors.push(`লাইন ${i + 1}: নাম লেখা হয়নি।`);
        continue;
      }

      if (keyword === 'SESSION' || kw[1] === 'সেশন') {
        curSession = { name, subjects: [] };
        const timePart = parts.slice(1).join(' ');
        const tm = toEnDigits(timePart).match(TIME_RANGE_RE);
        if (tm) {
          curSession.startTime = tm[1].padStart(5, '0');
          curSession.endTime = tm[2].padStart(5, '0');
        }
        result.sessions.push(curSession);
        curSubject = null;
        curChapter = null;
        result.stats.sessions++;
      } else if (keyword === 'SUBJECT' || kw[1] === 'বিষয়') {
        curSubject = { name, chapters: [] };
        for (const extra of parts.slice(1)) {
          if (toEnDigits(extra).match(TIME_RANGE_RE) || /AM|PM|টা/i.test(extra)) curSubject.timeSlot = extra;
          else curSubject.code = extra;
        }
        if (!curSession) {
          curSession = { name: '__CURRENT_SESSION__', subjects: [] };
          result.sessions.push(curSession);
        }
        curSession.subjects.push(curSubject);
        curChapter = null;
        result.stats.subjects++;
      } else {
        // CHAPTER
        curChapter = { name, lectures: [] };
        const totalPart = parts.slice(1).find((p) => /^\s*[\d০-৯]+\s*$/.test(p));
        if (totalPart) curChapter.totalLectures = parseInt(toEnDigits(totalPart), 10);
        if (!curSubject) {
          if (!curSession) {
            curSession = { name: '__CURRENT_SESSION__', subjects: [] };
            result.sessions.push(curSession);
          }
          curSubject = { name: '__CURRENT_SUBJECT__', chapters: [] };
          curSession.subjects.push(curSubject);
        }
        curSubject.chapters.push(curChapter);
        result.stats.chapters++;
      }
      continue;
    }

    // লেকচার লাইন
    const lec = parseLectureLine(line);
    if (lec) {
      if (curChapter) curChapter.lectures.push(lec);
      else result.orphanLectures.push(lec);
      result.stats.lectures++;
    } else {
      result.errors.push(`লাইন ${i + 1} বোঝা যায়নি: "${line.slice(0, 50)}${line.length > 50 ? '…' : ''}"`);
    }
  }

  if (
    result.stats.sessions === 0 && result.stats.subjects === 0 &&
    result.stats.chapters === 0 && result.stats.lectures === 0
  ) {
    result.errors.push('কোনো বৈধ SESSION/SUBJECT/CHAPTER/লেকচার লাইন পাওয়া যায়নি।');
  }

  return result;
}

/** পার্স করা ডেটা বর্তমান sessions-এর সাথে merge করে (কিছুই মুছে না) */
export function applyUpdateCode(
  existing: StudySession[],
  parsed: ParseResult,
  currentSessionId?: string,
  currentSubjectId?: string,
  currentChapterId?: string,
): { sessions: StudySession[]; summary: string[] } {
  const summary: string[] = [];

  if (parsed.mode === 'json' && parsed.jsonSessions) {
    summary.push(`JSON থেকে ${parsed.jsonSessions.length}টি সেশন দিয়ে সম্পূর্ণ ডেটা প্রতিস্থাপন হয়েছে।`);
    return { sessions: parsed.jsonSessions, summary };
  }

  // Deep copy
  const sessions: StudySession[] = JSON.parse(JSON.stringify(existing));
  let colorIdx = sessions.length;

  const findOrCreateSession = (ps: ParsedSession): StudySession => {
    if (ps.name === '__CURRENT_SESSION__') {
      const cur = sessions.find((s) => s.id === currentSessionId) || sessions[0];
      if (cur) return cur;
    }
    let s = sessions.find((x) => x.name.trim() === ps.name.trim());
    if (!s) {
      s = {
        id: genId('session'),
        name: ps.name,
        startTime: ps.startTime || '19:00',
        endTime: ps.endTime || '20:00',
        color: PALETTE[colorIdx++ % PALETTE.length],
        subjects: [],
      };
      sessions.push(s);
      summary.push(`✅ নতুন সেশন: "${ps.name}"`);
    } else {
      if (ps.startTime) s.startTime = ps.startTime;
      if (ps.endTime) s.endTime = ps.endTime;
    }
    return s;
  };

  const findOrCreateSubject = (session: StudySession, psub: ParsedSubject): Subject => {
    if (psub.name === '__CURRENT_SUBJECT__') {
      const cur = session.subjects.find((x) => x.id === currentSubjectId) || session.subjects[0];
      if (cur) return cur;
    }
    let sub = session.subjects.find((x) => x.name.trim() === psub.name.trim());
    if (!sub) {
      sub = {
        id: genId('sub'),
        name: psub.name,
        code: psub.code,
        timeSlot: psub.timeSlot || `${session.startTime} - ${session.endTime}`,
        color: PALETTE[colorIdx++ % PALETTE.length],
        chapters: [],
      };
      session.subjects.push(sub);
      summary.push(`✅ নতুন বিষয়: "${psub.name}" → ${session.name}`);
    } else {
      if (psub.code) sub.code = psub.code;
      if (psub.timeSlot) sub.timeSlot = psub.timeSlot;
    }
    return sub;
  };

  const findOrCreateChapter = (subject: Subject, pch: ParsedChapter): Chapter => {
    let ch = subject.chapters.find((x) => x.name.trim() === pch.name.trim());
    if (!ch) {
      ch = {
        id: genId('chap'),
        name: pch.name,
        totalLectures: pch.totalLectures || pch.lectures.length || 1,
        lectures: [],
        notes: '',
        createdAt: new Date().toISOString(),
      };
      subject.chapters.push(ch);
      summary.push(`✅ নতুন অধ্যায়: "${pch.name}" → ${subject.name}`);
    } else if (pch.totalLectures && pch.totalLectures > ch.totalLectures) {
      ch.totalLectures = pch.totalLectures;
    }
    return ch;
  };

  const mergeLectures = (chapter: Chapter, lectures: ParsedLecture[]) => {
    let added = 0;
    let updated = 0;
    for (const pl of lectures) {
      let target: Lecture | undefined;
      if (pl.number != null) target = chapter.lectures.find((l) => l.number === pl.number);
      if (target) {
        if (pl.title && !pl.title.startsWith('লেকচার ')) target.title = pl.title;
        if (pl.videoUrl) target.videoUrl = pl.videoUrl;
        updated++;
      } else {
        const nextNum = pl.number != null
          ? pl.number
          : (chapter.lectures.reduce((m, l) => Math.max(m, l.number), 0) + 1);
        chapter.lectures.push({
          id: genId('lec'),
          number: nextNum,
          title: pl.title || `লেকচার ${nextNum}`,
          videoUrl: pl.videoUrl,
          completed: false,
          notes: '',
        });
        added++;
      }
    }
    chapter.lectures.sort((a, b) => a.number - b.number);
    if (chapter.lectures.length > chapter.totalLectures) {
      chapter.totalLectures = chapter.lectures.length;
    }
    if (added) summary.push(`➕ ${added}টি নতুন লেকচার → "${chapter.name}"`);
    if (updated) summary.push(`🔄 ${updated}টি লেকচার আপডেট → "${chapter.name}"`);
  };

  for (const ps of parsed.sessions) {
    const session = findOrCreateSession(ps);
    for (const psub of ps.subjects) {
      const subject = findOrCreateSubject(session, psub);
      for (const pch of psub.chapters) {
        const chapter = findOrCreateChapter(subject, pch);
        mergeLectures(chapter, pch.lectures);
      }
    }
  }

  // Orphan লেকচার → বর্তমান অধ্যায়
  if (parsed.orphanLectures.length > 0) {
    const curSession = sessions.find((s) => s.id === currentSessionId) || sessions[0];
    const curSubject = curSession?.subjects.find((x) => x.id === currentSubjectId) || curSession?.subjects[0];
    const curChapter = curSubject?.chapters.find((x) => x.id === currentChapterId) || curSubject?.chapters[0];
    if (curChapter) {
      mergeLectures(curChapter, parsed.orphanLectures);
    } else {
      summary.push('⚠️ অধ্যায় নির্বাচিত না থাকায় কিছু লেকচার যোগ করা যায়নি।');
    }
  }

  if (summary.length === 0) summary.push('কোনো পরিবর্তন হয়নি (সব ডেটা আগে থেকেই আছে)।');

  return { sessions, summary };
}

export const SAMPLE_UPDATE_CODE = `# 📝 Smart Study আপডেট কোড — নমুনা
# '#' দিয়ে শুরু লাইন = মন্তব্য (উপেক্ষা হবে)

SESSION: সেশন ২: রাত ৯:০০ - ১০:০০ | 21:00 - 22:00
SUBJECT: রসায়ন (Chemistry) | CHE-101
CHAPTER: অধ্যায় ০১: মৌলের পর্যায়বৃত্ত ধর্ম | 12
1. পর্যায় সারণির ইতিহাস: https://www.youtube.com/watch?v=dQw4w9WgXcQ
2. ইলেকট্রন বিন্যাস: https://youtu.be/k3_tw44QsZQ
3. আয়নিকরণ শক্তি: https://www.youtube.com/watch?v=fJ9rUzIMcZQ

CHAPTER: অধ্যায় ০২: জৈব রসায়ন | 10
1. জৈব যৌগের শ্রেণিবিভাগ: https://www.youtube.com/watch?v=9bZkp7q19f0

# বর্তমানে খোলা অধ্যায়ে শুধু লেকচার যোগ করতে চাইলে
# SESSION/SUBJECT/CHAPTER ছাড়া সরাসরি লিখুন:
# 7. নতুন ক্লাস: https://youtu.be/XXXXX
`;
