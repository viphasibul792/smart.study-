import React, { useState } from 'react';
import { X, Clock, Palette, Check, Trash2 } from 'lucide-react';
import { StudySession } from '../types';
import { formatTimeSlot } from '../utils';

interface SessionEditorModalProps {
  session?: StudySession | null;
  onSave: (sessionData: { name: string; startTime: string; endTime: string; color: string }) => void;
  onDelete?: (sessionId: string) => void;
  onClose: () => void;
}

const COLOR_PRESETS = [
  { name: 'Blue', hex: '#3b82f6' },
  { name: 'Emerald', hex: '#10b981' },
  { name: 'Amber', hex: '#f59e0b' },
  { name: 'Purple', hex: '#a855f7' },
  { name: 'Rose', hex: '#f43f5e' },
  { name: 'Cyan', hex: '#06b6d4' },
];

export const SessionEditorModal: React.FC<SessionEditorModalProps> = ({
  session,
  onSave,
  onDelete,
  onClose,
}) => {
  const [name, setName] = useState(session?.name || '');
  const [startTime, setStartTime] = useState(session?.startTime || '19:00');
  const [endTime, setEndTime] = useState(session?.endTime || '20:00');
  const [color, setColor] = useState(session?.color || '#3b82f6');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave({
      name: name.trim(),
      startTime,
      endTime,
      color,
    });
  };

  const handleAutoName = () => {
    const formatted = formatTimeSlot(startTime, endTime);
    setName(`সেশন: ${formatted}`);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        className="relative w-full max-w-md bg-slate-900 border border-slate-700/80 rounded-2xl overflow-hidden shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/60">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/30 flex items-center justify-center text-blue-400">
              <Clock className="w-4 h-4" />
            </div>
            <h3 className="text-base font-bold text-white">
              {session ? 'সেশন সম্পাদনা করুন' : 'নতুন স্টাডি সেশন তৈরি'}
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Session Name */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-xs font-semibold text-slate-300">
                সেশনের নাম
              </label>
              <button
                type="button"
                onClick={handleAutoName}
                className="text-[11px] text-blue-400 hover:text-blue-300 hover:underline cursor-pointer"
              >
                সময় অনুযায়ী নাম দিন
              </button>
            </div>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="যেমন: সেশন ১: রাত ৭:০০ - ৮:০০"
              className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-blue-500"
            />
          </div>

          {/* Start & End Times */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                শুরুর সময়
              </label>
              <input
                type="time"
                required
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                শেষের সময়
              </label>
              <input
                type="time"
                required
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>

          {/* Color Presets */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-2 flex items-center gap-1.5">
              <Palette className="w-3.5 h-3.5 text-slate-400" />
              <span>সেশন থিম কালার</span>
            </label>
            <div className="flex items-center gap-2">
              {COLOR_PRESETS.map((preset) => (
                <button
                  key={preset.hex}
                  type="button"
                  onClick={() => setColor(preset.hex)}
                  className={`w-7 h-7 rounded-full transition-transform ${
                    color === preset.hex ? 'scale-125 ring-2 ring-white ring-offset-2 ring-offset-slate-900' : 'opacity-70 hover:opacity-100'
                  }`}
                  style={{ backgroundColor: preset.hex }}
                  title={preset.name}
                />
              ))}
            </div>
          </div>

          {/* Actions */}
          <div className="pt-3 border-t border-slate-800 flex items-center justify-between gap-3">
            {session && onDelete ? (
              <button
                type="button"
                onClick={() => onDelete(session.id)}
                className="px-3 py-2 text-xs font-medium text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded-xl transition-colors flex items-center gap-1"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>মুছে ফেলুন</span>
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
                className="px-5 py-2 text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white rounded-xl shadow-lg shadow-blue-600/30 flex items-center gap-1.5 transition-all cursor-pointer"
              >
                <Check className="w-3.5 h-3.5" />
                <span>{session ? 'আপডেট করুন' : 'তৈরি করুন'}</span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
