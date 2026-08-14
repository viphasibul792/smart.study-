import React, { useMemo, useState } from 'react';
import { X, Code2, Sparkles, Check, AlertCircle, ClipboardPaste, FileJson, Wand2 } from 'lucide-react';
import { StudySession } from '../types';
import { parseUpdateCode, applyUpdateCode, SAMPLE_UPDATE_CODE, ParseResult } from '../codeUpdater';

interface CodeUpdaterModalProps {
  sessions: StudySession[];
  currentSessionId?: string;
  currentSubjectId?: string;
  currentChapterId?: string;
  onApply: (sessions: StudySession[], summary: string[]) => void;
  onClose: () => void;
}

export const CodeUpdaterModal: React.FC<CodeUpdaterModalProps> = ({
  sessions,
  currentSessionId,
  currentSubjectId,
  currentChapterId,
  onApply,
  onClose,
}) => {
  const [code, setCode] = useState('');
  const [appliedSummary, setAppliedSummary] = useState<string[] | null>(null);

  const parsed: ParseResult | null = useMemo(() => {
    if (!code.trim()) return null;
    return parseUpdateCode(code);
  }, [code]);

  const canApply =
    !!parsed &&
    (parsed.mode === 'json'
      ? !!parsed.jsonSessions
      : parsed.stats.sessions + parsed.stats.subjects + parsed.stats.chapters + parsed.stats.lectures > 0);

  const handleApply = () => {
    if (!parsed || !canApply) return;
    const { sessions: next, summary } = applyUpdateCode(
      sessions,
      parsed,
      currentSessionId,
      currentSubjectId,
      currentChapterId,
    );
    onApply(next, summary);
    setAppliedSummary(summary);
    setCode('');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
      <div className="w-full max-w-3xl max-h-[92vh] overflow-hidden rounded-2xl bg-slate-900 border border-slate-700 shadow-2xl flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-700/80 bg-slate-900/95">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-violet-500/15 border border-violet-500/30">
              <Code2 className="w-5 h-5 text-violet-400" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100">কোড দিয়ে আপডেট করুন</h2>
              <p className="text-xs text-slate-400">
                সহজ কোড লিখে নতুন সেশন, বিষয়, অধ্যায়, ইউটিউব ক্লাস ও লেকচার যোগ/আপডেট করুন
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {appliedSummary ? (
            <div className="rounded-xl bg-emerald-500/10 border border-emerald-500/30 p-4 space-y-2">
              <div className="flex items-center gap-2 text-emerald-300 font-bold text-sm">
                <Check className="w-4 h-4" />
                আপডেট সফলভাবে প্রয়োগ হয়েছে!
              </div>
              <ul className="text-xs text-emerald-200/90 space-y-1 list-disc list-inside">
                {appliedSummary.map((s, i) => (
                  <li key={i}>{s}</li>
                ))}
              </ul>
              <div className="flex gap-2 pt-2">
                <button
                  onClick={() => setAppliedSummary(null)}
                  className="px-3 py-1.5 rounded-lg bg-violet-600 hover:bg-violet-500 text-white text-xs font-bold cursor-pointer"
                >
                  আরও আপডেট করুন
                </button>
                <button
                  onClick={onClose}
                  className="px-3 py-1.5 rounded-lg bg-slate-700 hover:bg-slate-600 text-slate-200 text-xs font-bold cursor-pointer"
                >
                  বন্ধ করুন
                </button>
              </div>
            </div>
          ) : (
            <>
              {/* ফরম্যাট গাইড */}
              <div className="rounded-xl bg-slate-800/60 border border-slate-700/70 p-4 text-xs text-slate-300 space-y-1.5">
                <p className="font-bold text-slate-200 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                  কোড ফরম্যাট:
                </p>
                <pre className="font-mono text-[11px] leading-relaxed text-slate-400 whitespace-pre-wrap bg-slate-950/60 rounded-lg p-3 border border-slate-800">
{`SESSION: সেশনের নাম | 21:00 - 22:00
SUBJECT: বিষয়ের নাম | কোড
CHAPTER: অধ্যায়ের নাম | মোট লেকচার
1. লেকচারের টাইটেল: https://youtube.com/watch?v=...
2. আরেকটি ক্লাস: https://youtu.be/...`}
                </pre>
                <ul className="list-disc list-inside text-slate-400 space-y-0.5">
                  <li>একই নামের সেশন/বিষয়/অধ্যায় থাকলে সেটিতেই যোগ হবে — নতুন নাম হলে নতুন তৈরি হবে</li>
                  <li>লেকচার নম্বর মিলে গেলে টাইটেল ও লিঙ্ক <b>আপডেট</b> হবে, না মিললে <b>নতুন যোগ</b> হবে</li>
                  <li>শুধু লেকচার লাইন লিখলে বর্তমানে খোলা অধ্যায়ে যোগ হবে</li>
                  <li className="flex items-center gap-1">
                    <FileJson className="w-3 h-3 text-blue-400" />
                    ব্যাকআপ JSON পেস্ট করলে সম্পূর্ণ ডেটা প্রতিস্থাপন হবে
                  </li>
                </ul>
              </div>

              {/* কোড ইনপুট */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-slate-300">আপডেট কোড লিখুন বা পেস্ট করুন:</label>
                  <button
                    onClick={() => setCode(SAMPLE_UPDATE_CODE)}
                    className="text-[11px] text-violet-400 hover:text-violet-300 flex items-center gap-1 cursor-pointer"
                  >
                    <Wand2 className="w-3 h-3" />
                    নমুনা কোড দেখুন
                  </button>
                </div>
                <textarea
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder={'SESSION: সেশন ২: রাত ৯:০০ - ১০:০০ | 21:00 - 22:00\nSUBJECT: রসায়ন (Chemistry) | CHE-101\nCHAPTER: অধ্যায় ০১ | 12\n1. প্রথম ক্লাস: https://youtu.be/XXXX'}
                  rows={10}
                  className="w-full rounded-xl bg-slate-950/80 border border-slate-700 focus:border-violet-500 focus:ring-1 focus:ring-violet-500/50 outline-none p-3.5 text-sm font-mono text-slate-200 placeholder:text-slate-600 resize-y"
                  spellCheck={false}
                />
              </div>

              {/* প্রিভিউ */}
              {parsed && (
                <div
                  className={`rounded-xl border p-4 text-xs space-y-2 ${
                    canApply
                      ? 'bg-emerald-500/5 border-emerald-500/25'
                      : 'bg-red-500/5 border-red-500/25'
                  }`}
                >
                  <div className="flex flex-wrap items-center gap-2">
                    {parsed.mode === 'json' ? (
                      <span className="px-2 py-0.5 rounded-full bg-blue-500/15 text-blue-300 border border-blue-500/30 font-bold">
                        JSON মোড — সম্পূর্ণ প্রতিস্থাপন
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded-full bg-violet-500/15 text-violet-300 border border-violet-500/30 font-bold">
                        কোড মোড — merge (কিছু মুছবে না)
                      </span>
                    )}
                    <span className="text-slate-400">
                      সেশন: <b className="text-slate-200">{parsed.stats.sessions}</b> • বিষয়:{' '}
                      <b className="text-slate-200">{parsed.stats.subjects}</b> • অধ্যায়:{' '}
                      <b className="text-slate-200">{parsed.stats.chapters}</b> • লেকচার:{' '}
                      <b className="text-slate-200">{parsed.stats.lectures}</b>
                    </span>
                  </div>
                  {parsed.errors.length > 0 && (
                    <div className="space-y-1">
                      {parsed.errors.slice(0, 5).map((err, i) => (
                        <p key={i} className="flex items-start gap-1.5 text-red-300">
                          <AlertCircle className="w-3.5 h-3.5 mt-0.5 shrink-0" />
                          {err}
                        </p>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        {!appliedSummary && (
          <div className="px-5 py-4 border-t border-slate-700/80 bg-slate-900/95 flex items-center justify-between gap-3">
            <p className="text-[11px] text-slate-500 flex items-center gap-1.5">
              <ClipboardPaste className="w-3.5 h-3.5" />
              নতুন ক্লাস এলে শুধু নতুন লাইনগুলো পেস্ট করলেই হবে
            </p>
            <div className="flex gap-2">
              <button
                onClick={onClose}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold transition-colors cursor-pointer"
              >
                বাতিল
              </button>
              <button
                onClick={handleApply}
                disabled={!canApply}
                className="px-5 py-2 rounded-xl bg-violet-600 hover:bg-violet-500 disabled:opacity-40 disabled:cursor-not-allowed text-white text-xs font-bold shadow-lg shadow-violet-600/25 transition-all cursor-pointer flex items-center gap-1.5"
              >
                <Check className="w-3.5 h-3.5" />
                আপডেট প্রয়োগ করুন
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
