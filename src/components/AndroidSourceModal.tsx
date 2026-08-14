import React, { useState } from 'react';
import { X, Smartphone, Copy, Check, Download, Github, Code, FileCode, FolderGit2 } from 'lucide-react';
import { ANDROID_PROJECT_FILES, AndroidFile } from '../androidSource';

interface AndroidSourceModalProps {
  onClose: () => void;
}

export const AndroidSourceModal: React.FC<AndroidSourceModalProps> = ({ onClose }) => {
  const [selectedFile, setSelectedFile] = useState<AndroidFile>(ANDROID_PROJECT_FILES[0]);
  const [copied, setCopied] = useState(false);
  const [downloaded, setDownloaded] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadAll = () => {
    // Generate combined bundle or individual downloadable text files
    const bundleContent = ANDROID_PROJECT_FILES.map(
      (f) => `========================================\n// FILE: ${f.path}\n// ${f.description}\n========================================\n\n${f.content}\n\n`
    ).join('\n');

    const blob = new Blob([bundleContent], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'StudyTracker_Android_Java_XML_Source.txt';
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    setDownloaded(true);
    setTimeout(() => setDownloaded(false), 3000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 md:p-6 bg-black/85 backdrop-blur-md animate-fadeIn">
      <div 
        className="relative w-full max-w-5xl bg-slate-900 border border-slate-700/80 rounded-2xl overflow-hidden shadow-2xl flex flex-col h-[90vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/80">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <Smartphone className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-white">
                  নেটিভ Android (Java + XML) সোর্স কোড
                </h3>
                <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-[10px] font-semibold border border-emerald-500/30">
                  Android Studio Ready
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                GitHub এ আপলোড করতে বা Android Studio তে সরাসরি প্রজেক্ট চালাতে কোডগুলো ব্যবহার করুন
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleDownloadAll}
              className="px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold flex items-center gap-1.5 shadow-md shadow-emerald-900/30 transition-all cursor-pointer"
            >
              {downloaded ? <Check className="w-3.5 h-3.5" /> : <Download className="w-3.5 h-3.5" />}
              <span>{downloaded ? 'ডাউনলোড সম্পন্ন ✓' : 'সব সোর্স ডাউনলোড'}</span>
            </button>
            <button
              onClick={onClose}
              className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* GitHub Instructions Banner */}
        <div className="bg-slate-950 px-6 py-2.5 border-b border-slate-800 flex flex-wrap items-center justify-between gap-2 text-xs">
          <div className="flex items-center gap-2 text-slate-300">
            <Github className="w-4 h-4 text-white" />
            <span>
              <strong>GitHub এ আপলোড করার ধাপ:</strong> ১. Android Studio তে প্রজেক্ট খুলুন → ২. <code className="bg-slate-800 px-1.5 py-0.5 rounded text-emerald-300">git init && git add . && git commit -m &quot;Init&quot;</code> → ৩. GitHub রেপো লিঙ্ক করে push করুন।
            </span>
          </div>
        </div>

        {/* Main Content: File list sidebar + Code Viewer */}
        <div className="flex-1 flex flex-col md:flex-row min-h-0 bg-slate-950">
          {/* File Explorer Sidebar */}
          <div className="w-full md:w-64 border-r border-slate-800 p-3 overflow-y-auto space-y-1 bg-slate-900/50 flex-shrink-0">
            <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-2 py-1 mb-1 flex items-center gap-1.5">
              <FolderGit2 className="w-3.5 h-3.5 text-blue-400" />
              <span>Project Files ({ANDROID_PROJECT_FILES.length})</span>
            </div>

            {ANDROID_PROJECT_FILES.map((file) => {
              const isSelected = selectedFile.name === file.name;
              return (
                <button
                  key={file.name}
                  onClick={() => setSelectedFile(file)}
                  className={`w-full text-left px-3 py-2 rounded-xl text-xs flex items-center gap-2 transition-all cursor-pointer ${
                    isSelected
                      ? 'bg-blue-600/20 text-blue-300 border border-blue-500/40 font-semibold'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                  }`}
                >
                  <FileCode className={`w-4 h-4 flex-shrink-0 ${file.language === 'java' ? 'text-amber-400' : 'text-blue-400'}`} />
                  <div className="truncate">
                    <div className="truncate">{file.name}</div>
                    <div className="text-[10px] text-slate-500 truncate">{file.language.toUpperCase()}</div>
                  </div>
                </button>
              );
            })}
          </div>

          {/* Code Viewer Panel */}
          <div className="flex-1 flex flex-col min-h-0 bg-slate-950">
            {/* File Path & Copy Toolbar */}
            <div className="flex items-center justify-between px-4 py-2.5 border-b border-slate-800/80 bg-slate-900/40">
              <div className="flex items-center gap-2 overflow-hidden">
                <Code className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                <span className="text-xs text-slate-300 font-mono truncate">
                  {selectedFile.path}
                </span>
              </div>
              <button
                onClick={handleCopy}
                className="px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium flex items-center gap-1.5 border border-slate-700 transition-colors cursor-pointer"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copied ? 'কপি হয়েছে ✓' : 'কোড কপি করুন'}</span>
              </button>
            </div>

            {/* Code Pre Container */}
            <div className="flex-1 p-4 overflow-auto font-mono text-xs text-slate-200 leading-relaxed bg-slate-950">
              <pre className="select-text">
                <code>{selectedFile.content}</code>
              </pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
