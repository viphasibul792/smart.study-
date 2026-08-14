import React, { useState } from 'react';
import { X, Link2, Sparkles, Check, AlertCircle, Copy } from 'lucide-react';
import { parseBulkLinks } from '../utils';

interface BulkLinkImporterModalProps {
  chapterName: string;
  totalLectures: number;
  onApplyLinks: (links: string[], autoExpand: boolean) => void;
  onClose: () => void;
}

export const BulkLinkImporterModal: React.FC<BulkLinkImporterModalProps> = ({
  chapterName,
  totalLectures,
  onApplyLinks,
  onClose,
}) => {
  const [rawText, setRawText] = useState('');
  const [autoExpandLectures, setAutoExpandLectures] = useState(true);

  const parsedLinks = parseBulkLinks(rawText);

  const sampleBulkText = `1. ভেক্টর রাশি ও স্কেলার রাশি: https://www.youtube.com/watch?v=dQw4w9WgXcQ
2. সামান্তরিক সূত্র: https://www.youtube.com/watch?v=k3_tw44QsZQ
3. লব্ধির মান ও দিক নির্ণয়: https://www.youtube.com/watch?v=fJ9rUzIMcZQ
4. ডট গুণন ও লম্ব ভেক্টর: https://www.youtube.com/watch?v=9bZkp7q19f0
5. ক্রস গুণন ও সামান্তরিকের ক্ষেত্রফল: https://www.youtube.com/watch?v=3JZ_D3ELwOQ
6. একক লম্ব ভেক্টর নির্ণয়: https://www.youtube.com/watch?v=L_LUpnjgPso
7. আপেক্ষিক বেগ ও বৃষ্টির অঙ্ক: https://www.youtube.com/watch?v=2Vv-BfVoq4g
8. নদী ও নৌকার ন্যূনতম দূরত্ব: https://www.youtube.com/watch?v=fRh_vgS2dFE
9. নদী পারাপার ন্যূনতম সময়: https://www.youtube.com/watch?v=hT_nvWreIhg
10. ক্যালকুলাস ও ভেক্টর ডিফারেনশিয়েশন: https://www.youtube.com/watch?v=uelHwf8o7_U`;

  const handleApply = () => {
    if (parsedLinks.length === 0) return;
    onApplyLinks(parsedLinks, autoExpandLectures);
    onClose();
  };

  const handleLoadSample = () => {
    setRawText(sampleBulkText);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        className="relative w-full max-w-2xl bg-slate-900 border border-slate-700/80 rounded-2xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-500/10 border border-blue-500/30 flex items-center justify-center text-blue-400">
              <Link2 className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">স্মার্ট লিঙ্ক ম্যাপিং (Bulk Link Importer)</h3>
              <p className="text-xs text-slate-400 mt-0.5">
                অধ্যায়: <span className="text-blue-400 font-semibold">{chapterName}</span> (বর্তমান লেকচার: {totalLectures}টি)
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-4">
          <div className="flex items-center justify-between">
            <label className="text-sm font-semibold text-slate-200 flex items-center gap-2">
              <span>সবকটি লিঙ্ক একসাথে পেস্ট করুন</span>
              <span className="text-xs font-normal text-slate-400">(YouTube, Drive, বা যেকোনো লিঙ্ক)</span>
            </label>
            <button
              onClick={handleLoadSample}
              type="button"
              className="text-xs font-medium text-blue-400 hover:text-blue-300 flex items-center gap-1 hover:underline cursor-pointer"
            >
              <Sparkles className="w-3.5 h-3.5" />
              ডেমো লিঙ্ক পেস্ট করুন
            </button>
          </div>

          <textarea
            value={rawText}
            onChange={(e) => setRawText(e.target.value)}
            placeholder={`উদাহরণ:\n1. https://youtu.be/abc12345\n2. https://youtube.com/watch?v=xyz9876\nhttps://youtu.be/...\nhttps://youtu.be/...`}
            rows={7}
            className="w-full bg-slate-950 border border-slate-700/80 rounded-xl p-3.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 font-mono"
          />

          {/* Parsing Feedback & Live Mapping Preview */}
          {parsedLinks.length > 0 && (
            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4 space-y-3">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-emerald-400 flex items-center gap-1.5">
                  <Check className="w-4 h-4 text-emerald-400" />
                  স্বয়ংক্রিয়ভাবে শনাক্ত হয়েছে: {parsedLinks.length}টি লিঙ্ক
                </span>
                <span className="text-slate-400">
                  লেকচার ১ থেকে {parsedLinks.length} পর্যন্ত ম্যাপ হবে
                </span>
              </div>

              {/* Preview Cards */}
              <div className="max-h-40 overflow-y-auto space-y-1.5 pr-1 text-xs">
                {parsedLinks.map((url, idx) => (
                  <div
                    key={idx}
                    className="flex items-center justify-between p-2 rounded-lg bg-slate-900/90 border border-slate-800/80 gap-2"
                  >
                    <span className="font-semibold text-blue-400 whitespace-nowrap">
                      লেকচার {idx + 1}
                    </span>
                    <span className="text-slate-400 truncate font-mono text-[11px] max-w-[360px]">
                      {url}
                    </span>
                    <span className="px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 text-[10px] font-medium border border-emerald-500/20 whitespace-nowrap">
                      ম্যাপ হবে ✓
                    </span>
                  </div>
                ))}
              </div>

              {/* Auto Adjust Option */}
              {parsedLinks.length > totalLectures && (
                <label className="flex items-center gap-2 pt-2 border-t border-slate-800/80 text-xs text-amber-300 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={autoExpandLectures}
                    onChange={(e) => setAutoExpandLectures(e.target.checked)}
                    className="rounded text-blue-600 focus:ring-blue-500 bg-slate-900 border-slate-700 w-4 h-4"
                  />
                  <span>
                    মোট লেকচার সংখ্যা স্বয়ংক্রিয়ভাবে <strong>{totalLectures}</strong> থেকে বাড়িয়ে <strong>{parsedLinks.length}</strong> করুন
                  </span>
                </label>
              )}
            </div>
          )}

          {rawText && parsedLinks.length === 0 && (
            <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl flex items-center gap-2 text-xs text-amber-300">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>কোনো বৈধ লিঙ্ক খুঁজে পাওয়া যায়নি। প্রতিটি লাইনে লিঙ্ক বা ইউটিউব URL দিন।</span>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-950 border-t border-slate-800 flex items-center justify-between gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-sm font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            বাতিল
          </button>
          <button
            onClick={handleApply}
            disabled={parsedLinks.length === 0}
            className="px-5 py-2.5 rounded-xl text-sm font-semibold bg-blue-600 hover:bg-blue-500 disabled:opacity-40 disabled:cursor-not-allowed text-white shadow-lg shadow-blue-600/30 flex items-center gap-2 transition-all cursor-pointer"
          >
            <Check className="w-4 h-4" />
            <span>{parsedLinks.length}টি লিঙ্ক ম্যাপ করুন</span>
          </button>
        </div>
      </div>
    </div>
  );
};
