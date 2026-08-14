import React, { useRef, useState } from 'react';
import { X, Cloud, Download, Upload, Check, RefreshCw, HardDrive, ShieldCheck, AlertCircle } from 'lucide-react';
import { StudySession, StudySettings } from '../types';

interface GoogleDriveSyncModalProps {
  sessions: StudySession[];
  settings: StudySettings;
  onRestoreData: (sessions: StudySession[], settings?: StudySettings) => void;
  onResetDefaults: () => void;
  onClose: () => void;
}

export const GoogleDriveSyncModal: React.FC<GoogleDriveSyncModalProps> = ({
  sessions,
  settings,
  onRestoreData,
  onResetDefaults,
  onClose,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [importStatus, setImportStatus] = useState<string | null>(null);
  const [copiedLink, setCopiedLink] = useState(false);

  // Compute total statistics
  let totalLecturesCount = 0;
  let completedLecturesCount = 0;
  let totalLinksCount = 0;

  sessions.forEach((s) => {
    s.subjects.forEach((sub) => {
      sub.chapters.forEach((chap) => {
        chap.lectures.forEach((lec) => {
          totalLecturesCount++;
          if (lec.completed) completedLecturesCount++;
          if (lec.videoUrl) totalLinksCount++;
        });
      });
    });
  });

  const handleExportBackup = () => {
    const backupData = {
      app: 'Study Reminder & Lecture Tracker',
      version: '1.0',
      exportedAt: new Date().toISOString(),
      stats: {
        totalSessions: sessions.length,
        totalLectures: totalLecturesCount,
        completedLectures: completedLecturesCount,
        mappedLinks: totalLinksCount,
      },
      settings,
      sessions,
    };

    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(backupData, null, 2));
    const downloadAnchor = document.createElement('a');
    const fileName = `study_tracker_backup_${new Date().toISOString().slice(0, 10)}.json`;
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute('download', fileName);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
    setImportStatus('✅ ব্যাকআপ ফাইল সফলভাবে ডাউনলোড হয়েছে!');
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const parsed = JSON.parse(event.target?.result as string);
        if (parsed.sessions && Array.isArray(parsed.sessions)) {
          onRestoreData(parsed.sessions, parsed.settings);
          setImportStatus(`✅ সফলভাবে ${parsed.sessions.length}টি সেশনের ডেটা রিস্টোর করা হয়েছে!`);
          setTimeout(() => {
            onClose();
          }, 1200);
        } else {
          setImportStatus('❌ ফাইলের ফরম্যাট সঠিক নয়। সঠিক ব্যাকআপ JSON ফাইল আপলোড করুন।');
        }
      } catch {
        setImportStatus('❌ ব্যাকআপ ফাইল পড়া সম্ভব হয়নি।');
      }
    };
    reader.readAsText(file);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        className="relative w-full max-w-lg bg-slate-900 border border-slate-700/80 rounded-2xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/60">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <Cloud className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">ডেটা ব্যাকআপ ও ক্লাউড সিঙ্ক</h3>
              <p className="text-xs text-slate-400">Google Drive & লোকাল ডিভাইস ব্যাকআপ</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-5 overflow-y-auto">
          {/* Current Status Overview */}
          <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs text-slate-400">লোকাল ডেটা স্টোরেজ:</span>
              <span className="text-xs font-semibold text-emerald-400 flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5" />
                নিরাপদে সংরক্ষিত (LocalStorage)
              </span>
            </div>
            <div className="grid grid-cols-3 gap-2 pt-2 border-t border-slate-800/80 text-center">
              <div className="bg-slate-900/60 p-2 rounded-lg">
                <div className="text-base font-bold text-white">{sessions.length}</div>
                <div className="text-[10px] text-slate-400">সেশন</div>
              </div>
              <div className="bg-slate-900/60 p-2 rounded-lg">
                <div className="text-base font-bold text-blue-400">{totalLinksCount}</div>
                <div className="text-[10px] text-slate-400">ম্যাপ করা লিঙ্ক</div>
              </div>
              <div className="bg-slate-900/60 p-2 rounded-lg">
                <div className="text-base font-bold text-emerald-400">{completedLecturesCount}</div>
                <div className="text-[10px] text-slate-400">সম্পন্ন ক্লাস</div>
              </div>
            </div>
          </div>

          {/* Export / Download Backup Button */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-2 flex items-center gap-1.5">
              <HardDrive className="w-4 h-4 text-emerald-400" />
              <span>১. Google Drive এ সংরক্ষণ ও ব্যাকআপ ফাইল তৈরি</span>
            </label>
            <button
              type="button"
              onClick={handleExportBackup}
              className="w-full py-3 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs flex items-center justify-center gap-2 shadow-lg shadow-emerald-600/20 transition-all cursor-pointer"
            >
              <Download className="w-4 h-4" />
              <span>ব্যাকআপ ফাইল ডাউনলোড করুন (.json)</span>
            </button>
            <p className="text-[11px] text-slate-400 mt-1.5 leading-relaxed">
              এই ফাইলটি আপনার Google Drive বা ফোনে রেখে দিন। পরে যেকোনো সময় রিস্টোর করতে পারবেন।
            </p>
          </div>

          {/* Import / Restore Button */}
          <div className="pt-3 border-t border-slate-800">
            <label className="block text-xs font-semibold text-slate-300 mb-2 flex items-center gap-1.5">
              <Upload className="w-4 h-4 text-blue-400" />
              <span>২. পূর্বের ব্যাকআপ ফাইল থেকে রিস্টোর করুন</span>
            </label>
            <input
              type="file"
              ref={fileInputRef}
              accept=".json"
              onChange={handleFileSelect}
              className="hidden"
            />
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="w-full py-3 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 font-semibold text-xs flex items-center justify-center gap-2 transition-all cursor-pointer"
            >
              <Upload className="w-4 h-4" />
              <span>ব্যাকআপ ফাইল সিলেক্ট করুন (.json)</span>
            </button>
          </div>

          {importStatus && (
            <div className="p-3 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-emerald-400 flex-shrink-0" />
              <span>{importStatus}</span>
            </div>
          )}

          {/* Reset to Default */}
          <div className="pt-3 border-t border-slate-800 flex items-center justify-between">
            <div>
              <div className="text-xs font-semibold text-slate-400">ডিফল্ট ডেটা রিস্টোর</div>
              <div className="text-[10px] text-slate-500">৩টি মূল সেশন ও ডেমো লেকচার ফিরে পেতে</div>
            </div>
            <button
              type="button"
              onClick={() => {
                if (window.confirm('আপনি কি নিশ্চিত যে সকল ডেটা রিসেট করে ডিফল্ট সেটিংসে ফিরতে চান?')) {
                  onResetDefaults();
                  onClose();
                }
              }}
              className="px-3 py-1.5 rounded-lg text-xs font-medium text-amber-400 hover:bg-amber-500/10 border border-amber-500/30 transition-colors flex items-center gap-1"
            >
              <RefreshCw className="w-3 h-3" />
              <span>রিসেট করুন</span>
            </button>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-950 border-t border-slate-800 flex items-center justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
          >
            বন্ধ করুন
          </button>
        </div>
      </div>
    </div>
  );
};
