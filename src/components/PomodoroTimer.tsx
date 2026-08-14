import React, { useState, useEffect } from 'react';
import { Play, Pause, RotateCcw, Bell, CheckCircle2, Volume2, Sparkles } from 'lucide-react';
import { soundManager } from '../utils';

export const PomodoroTimer: React.FC = () => {
  const [timeLeft, setTimeLeft] = useState(25 * 60); // 25 minutes
  const [isRunning, setIsRunning] = useState(false);
  const [mode, setMode] = useState<'study' | 'break'>('study');
  const [completedSessions, setCompletedSessions] = useState(0);

  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (isRunning && timeLeft > 0) {
      timer = setInterval(() => {
        setTimeLeft((prev) => prev - 1);
      }, 1000);
    } else if (isRunning && timeLeft === 0) {
      soundManager.playAlarmChime();
      if (mode === 'study') {
        setCompletedSessions((prev) => prev + 1);
        setMode('break');
        setTimeLeft(5 * 60); // 5 min break
      } else {
        setMode('study');
        setTimeLeft(25 * 60);
      }
      setIsRunning(false);
    }
    return () => clearInterval(timer);
  }, [isRunning, timeLeft, mode]);

  const togglePlay = () => {
    if (!isRunning) {
      soundManager.playSuccessChime();
    }
    setIsRunning(!isRunning);
  };

  const handleReset = () => {
    setIsRunning(false);
    setTimeLeft(mode === 'study' ? 25 * 60 : 5 * 60);
  };

  const setStudyDuration = (mins: number) => {
    setIsRunning(false);
    setMode('study');
    setTimeLeft(mins * 60);
  };

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  const formattedTime = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

  const progressPercent = mode === 'study' 
    ? ((25 * 60 - timeLeft) / (25 * 60)) * 100 
    : ((5 * 60 - timeLeft) / (5 * 60)) * 100;

  return (
    <div className="bg-slate-900/90 border border-slate-800/80 rounded-2xl p-4 md:p-5 shadow-xl">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <div className={`w-2.5 h-2.5 rounded-full ${isRunning ? 'bg-emerald-400 animate-ping' : 'bg-slate-500'}`} />
          <span className="text-xs font-bold text-white uppercase tracking-wider">
            {mode === 'study' ? '🎯 ফোকাস স্টাডি টাইমার' : '☕ রিলাক্স ব্রেক'}
          </span>
        </div>
        <div className="flex items-center gap-1 text-[11px] text-slate-400">
          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
          <span>{completedSessions}টি ফোকাস সেশন</span>
        </div>
      </div>

      {/* Main Timer Display */}
      <div className="flex flex-col items-center justify-center py-2">
        <div className="text-4xl font-extrabold text-white font-mono tracking-wider">
          {formattedTime}
        </div>
        
        {/* Progress Line */}
        <div className="w-full bg-slate-800 h-1.5 rounded-full mt-3 overflow-hidden">
          <div 
            className={`h-full transition-all duration-300 ${mode === 'study' ? 'bg-blue-500' : 'bg-emerald-500'}`}
            style={{ width: `${progressPercent}%` }}
          />
        </div>

        {/* Quick Presets */}
        <div className="flex items-center gap-1.5 mt-3">
          <button
            onClick={() => setStudyDuration(25)}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-medium transition-colors cursor-pointer ${
              mode === 'study' && timeLeft === 25 * 60 ? 'bg-blue-500/20 text-blue-300 border border-blue-500/40' : 'text-slate-400 hover:text-white bg-slate-800/60'
            }`}
          >
            ২৫ মিনিট
          </button>
          <button
            onClick={() => setStudyDuration(45)}
            className="px-2.5 py-1 rounded-lg text-[11px] font-medium text-slate-400 hover:text-white bg-slate-800/60 transition-colors cursor-pointer"
          >
            ৪৫ মিনিট
          </button>
          <button
            onClick={() => {
              setIsRunning(false);
              setMode('break');
              setTimeLeft(5 * 60);
            }}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-medium transition-colors cursor-pointer ${
              mode === 'break' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' : 'text-slate-400 hover:text-white bg-slate-800/60'
            }`}
          >
            ৫ মি. ব্রেক
          </button>
        </div>

        {/* Controls */}
        <div className="flex items-center gap-3 mt-4">
          <button
            onClick={togglePlay}
            className={`px-5 py-2 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all shadow-lg cursor-pointer ${
              isRunning
                ? 'bg-amber-500 hover:bg-amber-400 text-slate-950 shadow-amber-500/20'
                : 'bg-emerald-500 hover:bg-emerald-400 text-slate-950 shadow-emerald-500/20'
            }`}
          >
            {isRunning ? <Pause className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current" />}
            <span>{isRunning ? 'পজ করুন' : 'শুরু করুন'}</span>
          </button>

          <button
            onClick={handleReset}
            title="রিসেট"
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors cursor-pointer"
          >
            <RotateCcw className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
