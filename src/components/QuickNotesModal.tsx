import React, { useState } from 'react';
import { X, BookOpen, Save, Sparkles, Check } from 'lucide-react';

interface QuickNotesModalProps {
  title: string;
  subtitle: string;
  initialNotes: string;
  onSave: (notes: string) => void;
  onClose: () => void;
}

export const QuickNotesModal: React.FC<QuickNotesModalProps> = ({
  title,
  subtitle,
  initialNotes,
  onSave,
  onClose,
}) => {
  const [notes, setNotes] = useState(initialNotes || '');
  const [savedBadge, setSavedBadge] = useState(false);

  const mathSymbols = [
    '√(P² + Q² + 2PQ cos α)',
    'A · B = |A||B| cos θ',
    'A × B = |A||B| sin θ η̂',
    'H_max = (v₀² sin² θ) / 2g',
    'd/dx',
    '∫',
    'θ',
    'α',
    'λ',
    'Δ',
    '±',
    '≠',
    '≈',
    '²',
    '³',
  ];

  const handleInsertSymbol = (sym: string) => {
    setNotes((prev) => prev + (prev.endsWith(' ') || prev.length === 0 ? '' : ' ') + sym + ' ');
  };

  const handleSave = () => {
    onSave(notes);
    setSavedBadge(true);
    setTimeout(() => {
      setSavedBadge(false);
      onClose();
    }, 400);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        className="relative w-full max-w-xl bg-slate-900 border border-slate-700/80 rounded-2xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/60">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
              <BookOpen className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">কুইক নোটস ও সূত্র ভান্ডার (Quick Notes)</h3>
              <p className="text-xs text-slate-400">{title} • {subtitle}</p>
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
        <div className="p-6 space-y-4 overflow-y-auto">
          {/* Quick Symbol Picker */}
          <div>
            <label className="block text-[11px] font-semibold text-slate-400 mb-2 flex items-center gap-1">
              <Sparkles className="w-3 h-3 text-amber-400" />
              <span>প্রয়োজনীয় গাণিতিক প্রতীক ও সূত্র যোগ করুন:</span>
            </label>
            <div className="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto pr-1">
              {mathSymbols.map((sym, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={() => handleInsertSymbol(sym)}
                  className="px-2.5 py-1 text-xs bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg border border-slate-700/80 transition-colors font-mono cursor-pointer"
                >
                  {sym}
                </button>
              ))}
            </div>
          </div>

          <div>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="ক্লাসের গুরুত্বপূর্ণ সূত্র, শর্টকাট টেকনিক বা পয়েন্ট লিখে রাখুন..."
              rows={9}
              className="w-full bg-slate-950 border border-slate-700/80 rounded-xl p-3.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-amber-500 focus:ring-1 focus:ring-amber-500 leading-relaxed font-sans"
            />
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-950 border-t border-slate-800 flex items-center justify-between gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800 rounded-xl transition-colors"
          >
            বাতিল
          </button>
          <button
            onClick={handleSave}
            className="px-5 py-2.5 text-xs font-semibold bg-amber-500 hover:bg-amber-400 text-slate-950 rounded-xl shadow-lg shadow-amber-500/20 flex items-center gap-1.5 transition-all cursor-pointer font-bold"
          >
            {savedBadge ? <Check className="w-4 h-4 text-emerald-950" /> : <Save className="w-4 h-4" />}
            <span>{savedBadge ? 'সংরক্ষিত হয়েছে ✓' : 'নোটস সেভ করুন'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
