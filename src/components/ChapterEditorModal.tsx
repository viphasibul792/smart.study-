import React, { useState } from 'react';
import { X, Layers, Hash, Check, Trash2 } from 'lucide-react';
import { Chapter } from '../types';

interface ChapterEditorModalProps {
  chapter?: Chapter | null;
  subjectName: string;
  onSave: (data: { name: string; chapterNumber: string; totalLectures: number }) => void;
  onDelete?: (chapterId: string) => void;
  onClose: () => void;
}

export const ChapterEditorModal: React.FC<ChapterEditorModalProps> = ({
  chapter,
  subjectName,
  onSave,
  onDelete,
  onClose,
}) => {
  const [name, setName] = useState(chapter?.name || '');
  const [chapterNumber, setChapterNumber] = useState(chapter?.chapterNumber || '০২');
  const [totalLectures, setTotalLectures] = useState(chapter?.totalLectures || 20);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || totalLectures <= 0) return;
    onSave({
      name: name.trim(),
      chapterNumber: chapterNumber.trim(),
      totalLectures: Number(totalLectures),
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        className="relative w-full max-w-md bg-slate-900 border border-slate-700/80 rounded-2xl overflow-hidden shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/60">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-purple-500/10 border border-purple-500/30 flex items-center justify-center text-purple-400">
              <Layers className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">
                {chapter ? 'অধ্যায় সম্পাদনা করুন' : 'নতুন অধ্যায় যুক্ত করুন'}
              </h3>
              <p className="text-[11px] text-slate-400">বিষয়: {subjectName}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Chapter Name */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              অধ্যায়ের শিরোনাম (Chapter Title)
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="যেমন: অধ্যায় ০২: ভেক্টর"
              className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-purple-500"
            />
          </div>

          {/* Chapter Number & Total Lectures Input */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                অধ্যায় নং (Code)
              </label>
              <input
                type="text"
                value={chapterNumber}
                onChange={(e) => setChapterNumber(e.target.value)}
                placeholder="যেমন: ০২ বা 02"
                className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-purple-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1">
                <Hash className="w-3.5 h-3.5 text-purple-400" />
                <span>Total Lectures</span>
              </label>
              <input
                type="number"
                required
                min={1}
                max={150}
                value={totalLectures}
                onChange={(e) => setTotalLectures(Math.max(1, parseInt(e.target.value, 10) || 1))}
                className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-purple-500 font-bold text-purple-300"
              />
            </div>
          </div>

          <div className="p-3 bg-purple-500/10 border border-purple-500/20 rounded-xl text-xs text-purple-200">
            💡 <strong>অটোমেটিক লেকচার জেনারেটর:</strong> &quot;Total Lectures&quot; ঘরে সংখ্যা দিলে অ্যাপটি স্বয়ংক্রিয়ভাবে Lecture 1 থেকে Lecture {totalLectures} পর্যন্ত বাটন তৈরি করবে।
          </div>

          {/* Actions */}
          <div className="pt-3 border-t border-slate-800 flex items-center justify-between gap-3">
            {chapter && onDelete ? (
              <button
                type="button"
                onClick={() => onDelete(chapter.id)}
                className="px-3 py-2 text-xs font-medium text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded-xl transition-colors flex items-center gap-1"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>মুছুন</span>
              </button>
            ) : (
              <div />
            )}

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800 rounded-xl transition-colors"
              >
                বাতিল
              </button>
              <button
                type="submit"
                className="px-5 py-2 text-xs font-semibold bg-purple-600 hover:bg-purple-500 text-white rounded-xl shadow-lg shadow-purple-600/30 flex items-center gap-1.5 transition-all cursor-pointer"
              >
                <Check className="w-3.5 h-3.5" />
                <span>{chapter ? 'আপডেট করুন' : 'তৈরি করুন'}</span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
