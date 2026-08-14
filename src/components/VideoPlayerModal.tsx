import React from 'react';
import { X, ExternalLink, CheckCircle, Youtube, BookOpen } from 'lucide-react';
import { Lecture } from '../types';
import { extractYouTubeId } from '../utils';

interface VideoPlayerModalProps {
  lecture: Lecture;
  chapterName: string;
  subjectName: string;
  onClose: () => void;
  onToggleComplete: (lectureId: string) => void;
  onOpenNotes: (lecture: Lecture) => void;
}

export const VideoPlayerModal: React.FC<VideoPlayerModalProps> = ({
  lecture,
  chapterName,
  subjectName,
  onClose,
  onToggleComplete,
  onOpenNotes,
}) => {
  const youtubeId = extractYouTubeId(lecture.videoUrl);

  const handleOpenExternal = () => {
    if (lecture.videoUrl) {
      window.open(lecture.videoUrl, '_blank', 'noopener,noreferrer');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        className="relative w-full max-w-4xl bg-slate-900 border border-slate-700/60 rounded-2xl overflow-hidden shadow-2xl flex flex-col max-h-[92vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800 bg-slate-950/60">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center justify-center text-red-400">
              <Youtube className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  {subjectName}
                </span>
                <span className="text-xs text-slate-400">{chapterName}</span>
              </div>
              <h3 className="text-lg font-bold text-white mt-0.5">{lecture.title}</h3>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleOpenExternal}
              title="YouTube অ্যাপ বা নতুন ট্যাবে খুলুন"
              className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium flex items-center gap-1.5 transition-colors border border-slate-700"
            >
              <ExternalLink className="w-3.5 h-3.5" />
              <span>YouTube App</span>
            </button>
            <button
              onClick={onClose}
              className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Video Player Container */}
        <div className="relative w-full bg-black aspect-video flex items-center justify-center">
          {youtubeId ? (
            <iframe
              src={`https://www.youtube.com/embed/${youtubeId}?autoplay=1&rel=0&modestbranding=1`}
              title={lecture.title}
              className="w-full h-full border-0"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
            />
          ) : lecture.videoUrl ? (
            <div className="p-8 text-center max-w-md">
              <p className="text-slate-300 mb-4 font-medium">
                এই লিঙ্কটি সরাসরি ভিডিও হিসেবে এমবেডযোগ্য নয় বা কাস্টম ড্রাইভ/ওয়েব লিঙ্ক:
              </p>
              <p className="text-xs text-slate-400 break-all bg-slate-800/80 p-3 rounded-lg border border-slate-700 mb-5">
                {lecture.videoUrl}
              </p>
              <button
                onClick={handleOpenExternal}
                className="w-full py-3 px-4 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold rounded-xl flex items-center justify-center gap-2 shadow-lg shadow-emerald-900/30 transition-all"
              >
                <ExternalLink className="w-4 h-4" />
                ব্রাউজারে ভিডিও চালু করুন
              </button>
            </div>
          ) : (
            <div className="text-center p-8">
              <p className="text-slate-400 mb-2">এই লেকচারের কোনো ভিডিও লিঙ্ক যুক্ত করা হয়নি।</p>
              <p className="text-xs text-slate-500">
                বাল্ক লিঙ্ক ইম্পোর্টার ব্যবহার করে এক ক্লিকে লিঙ্ক যুক্ত করুন।
              </p>
            </div>
          )}
        </div>

        {/* Action Bar Footer */}
        <div className="p-4 bg-slate-950 border-t border-slate-800 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <button
              onClick={() => onToggleComplete(lecture.id)}
              className={`px-4 py-2 rounded-xl text-sm font-semibold flex items-center gap-2 transition-all ${
                lecture.completed
                  ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/20'
                  : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700'
              }`}
            >
              <CheckCircle className={`w-4 h-4 ${lecture.completed ? 'text-white' : 'text-slate-400'}`} />
              <span>{lecture.completed ? 'ক্লাস দেখা সম্পন্ন ✓' : 'সম্পন্ন চিহ্নিত করুন'}</span>
            </button>

            <button
              onClick={() => onOpenNotes(lecture)}
              className="px-3.5 py-2 rounded-xl text-sm font-medium bg-slate-800/80 hover:bg-slate-700 text-slate-300 border border-slate-700/80 flex items-center gap-2 transition-colors"
            >
              <BookOpen className="w-4 h-4 text-amber-400" />
              <span>লেকচার নোটস {lecture.notes ? '📝' : ''}</span>
            </button>
          </div>

          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-sm font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors ml-auto"
          >
            বন্ধ করুন
          </button>
        </div>
      </div>
    </div>
  );
};
