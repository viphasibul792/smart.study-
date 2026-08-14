import React, { useState } from 'react';
import { X, Book, Clock, Check, Trash2, Palette } from 'lucide-react';
import { Subject } from '../types';

interface SubjectEditorModalProps {
  subject?: Subject | null;
  sessionName: string;
  onSave: (data: { name: string; code: string; timeSlot: string; color: string }) => void;
  onDelete?: (subjectId: string) => void;
  onClose: () => void;
}

const SUBJECT_COLORS = ['#38bdf8', '#34d399', '#fbbf24', '#f472b6', '#a78bfa', '#fb923c'];

export const SubjectEditorModal: React.FC<SubjectEditorModalProps> = ({
  subject,
  sessionName,
  onSave,
  onDelete,
  onClose,
}) => {
  const [name, setName] = useState(subject?.name || '');
  const [code, setCode] = useState(subject?.code || '');
  const [timeSlot, setTimeSlot] = useState(subject?.timeSlot || '৮:০০ PM - ৯:০০ PM');
  const [color, setColor] = useState(subject?.color || '#38bdf8');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave({
      name: name.trim(),
      code: code.trim(),
      timeSlot: timeSlot.trim(),
      color,
    });
  };

  const quickSubjects = [
    { name: 'পদার্থবিজ্ঞান (Physics)', code: 'PHY-101' },
    { name: 'উচ্চতর গণিত (Higher Math)', code: 'HM-101' },
    { name: 'রসায়ন (Chemistry)', code: 'CHEM-101' },
    { name: 'জীববিজ্ঞান (Biology)', code: 'BIO-101' },
    { name: 'তথ্য ও যোগাযোগ প্রযুক্তি (ICT)', code: 'ICT-101' },
    { name: 'ইংরেজি (English)', code: 'ENG-101' },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        className="relative w-full max-w-md bg-slate-900 border border-slate-700/80 rounded-2xl overflow-hidden shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/60">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <Book className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">
                {subject ? 'বিষয় সম্পাদনা করুন' : 'নতুন বিষয় যুক্ত করুন'}
              </h3>
              <p className="text-[11px] text-slate-400">সেশন: {sessionName}</p>
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
          {/* Quick Select */}
          {!subject && (
            <div>
              <label className="block text-[11px] font-semibold text-slate-400 mb-1.5">
                দ্রুত বিষয় নির্বাচন করুন:
              </label>
              <div className="flex flex-wrap gap-1.5">
                {quickSubjects.map((q) => (
                  <button
                    key={q.name}
                    type="button"
                    onClick={() => {
                      setName(q.name);
                      setCode(q.code);
                    }}
                    className="px-2.5 py-1 text-xs rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors"
                  >
                    {q.name.split(' ')[0]}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Subject Name */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              বিষয়ের নাম (Subject Name)
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="যেমন: পদার্থবিজ্ঞান (Physics)"
              className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-emerald-500"
            />
          </div>

          {/* Custom Time Slot */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-emerald-400" />
              <span>পড়ার নির্দিষ্ট সময় সূচি (Custom Time Slot)</span>
            </label>
            <input
              type="text"
              required
              value={timeSlot}
              onChange={(e) => setTimeSlot(e.target.value)}
              placeholder="যেমন: ৮:০০ PM - ৯:০০ PM"
              className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-emerald-500"
            />
            <p className="text-[11px] text-slate-500 mt-1">
              প্রতি বিষয়ের পাশে পড়ার নির্দিষ্ট সময় কাস্টমাইজ করতে পারেন
            </p>
          </div>

          {/* Color */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-2 flex items-center gap-1.5">
              <Palette className="w-3.5 h-3.5 text-slate-400" />
              <span>ট্যাগ কালার</span>
            </label>
            <div className="flex items-center gap-2">
              {SUBJECT_COLORS.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setColor(c)}
                  className={`w-7 h-7 rounded-full transition-transform ${
                    color === c ? 'scale-125 ring-2 ring-white ring-offset-2 ring-offset-slate-900' : 'opacity-70 hover:opacity-100'
                  }`}
                  style={{ backgroundColor: c }}
                />
              ))}
            </div>
          </div>

          {/* Actions */}
          <div className="pt-3 border-t border-slate-800 flex items-center justify-between gap-3">
            {subject && onDelete ? (
              <button
                type="button"
                onClick={() => onDelete(subject.id)}
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
                className="px-5 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl shadow-lg shadow-emerald-600/30 flex items-center gap-1.5 transition-all cursor-pointer"
              >
                <Check className="w-3.5 h-3.5" />
                <span>{subject ? 'আপডেট করুন' : 'যুক্ত করুন'}</span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
