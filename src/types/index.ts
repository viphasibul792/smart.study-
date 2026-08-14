export interface Lecture {
  id: string;
  number: number;
  title: string;
  videoUrl: string;
  completed: boolean;
  notes: string;
  completedAt?: string;
}

export interface Chapter {
  id: string;
  name: string;
  chapterNumber?: string;
  totalLectures: number;
  lectures: Lecture[];
  notes: string;
  createdAt: string;
}

export interface Subject {
  id: string;
  name: string;
  code?: string;
  timeSlot: string; // e.g. "8:00 PM - 9:00 PM"
  color: string;
  chapters: Chapter[];
}

export interface StudySession {
  id: string;
  name: string; // e.g. "সেশন ১: রাত ৭:০০ - ৮:০০"
  startTime: string; // "19:00"
  endTime: string; // "20:00"
  color: string;
  subjects: Subject[];
}

export interface StudySettings {
  soundEnabled: boolean;
  notificationsEnabled: boolean;
  reminderMinutesBefore: number;
  language: 'bn' | 'en';
  darkMode: boolean;
}

export interface BulkImportResult {
  links: string[];
  totalParsed: number;
}
