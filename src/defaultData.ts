import { StudySession, StudySettings } from './types';

export const INITIAL_SETTINGS: StudySettings = {
  soundEnabled: true,
  notificationsEnabled: true,
  reminderMinutesBefore: 5,
  language: 'bn',
  darkMode: true,
};

const createLectures = (count: number, urls: string[] = [], completedCount = 0) => {
  return Array.from({ length: count }, (_, i) => ({
    id: `lec-${i + 1}-${Math.random().toString(36).substring(2, 7)}`,
    number: i + 1,
    title: `লেকচার ${i + 1}`,
    videoUrl: urls[i] || '',
    completed: i < completedCount,
    notes: i === 0 ? 'ভেক্টর রাশি এবং স্কেলার রাশির প্রাথমিক পার্থক্য ও ডট গুণন সূত্র: A · B = |A||B| cos θ' : '',
    completedAt: i < completedCount ? new Date().toISOString() : undefined,
  }));
};

const samplePhysicsUrls = [
  'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
  'https://www.youtube.com/watch?v=k3_tw44QsZQ',
  'https://www.youtube.com/watch?v=fJ9rUzIMcZQ',
];

export const INITIAL_SESSIONS: StudySession[] = [
  {
    id: 'session-1',
    name: 'সেশন ১: রাত ৭:০০ - ৮:০০',
    startTime: '19:00',
    endTime: '20:00',
    color: '#3b82f6',
    subjects: [
      {
        id: 'sub-physics',
        name: 'পদার্থবিজ্ঞান (Physics)',
        code: 'PHY-101',
        timeSlot: '৭:০০ PM - ৮:০০ PM',
        color: '#38bdf8',
        chapters: [
          {
            id: 'chap-vector',
            name: 'অধ্যায় ০২: ভেক্টর',
            chapterNumber: '০২',
            totalLectures: 20,
            lectures: createLectures(20, samplePhysicsUrls, 6),
            notes: '📌 গুরুত্বপূর্ণ সূত্রসমূহ:\n১. ভেক্টর যোগের সামান্তরিক সূত্র: R = √(P² + Q² + 2PQ cos α)\n২. লব্ধির দিক: tan θ = (Q sin α) / (P + Q cos α)\n৩. ডট গুণন: A · B = AxBx + AyBy + AzBz\n৪. ক্রস গুণন: A × B = |A||B| sin θ η̂',
            createdAt: new Date().toISOString(),
          },
          {
            id: 'chap-dynamics',
            name: 'অধ্যায় ০৩: গতিবিদ্যা',
            chapterNumber: '০৩',
            totalLectures: 14,
            lectures: createLectures(14, [], 2),
            notes: 'প্রাস (Projectile Motion): H_max = (v₀² sin² θ) / 2g, R = (v₀² sin 2θ) / g',
            createdAt: new Date().toISOString(),
          }
        ],
      },
      {
        id: 'sub-ict',
        name: 'তথ্য ও যোগাযোগ প্রযুক্তি (ICT)',
        code: 'ICT-101',
        timeSlot: '৮:০০ PM - ৮:৩০ PM',
        color: '#a855f7',
        chapters: [
          {
            id: 'chap-num-system',
            name: 'অধ্যায় ০৩: সংখ্যা পদ্ধতি ও ডিজিটাল ডিভাইস',
            chapterNumber: '০৩',
            totalLectures: 10,
            lectures: createLectures(10, [], 4),
            notes: '২ এর পরিপূরক (2\'s Complement) এবং বুলিয়ান অ্যালজেবরা সূত্রাবলী।',
            createdAt: new Date().toISOString(),
          }
        ],
      }
    ],
  },
  {
    id: 'session-2',
    name: 'সেশন ২: রাত ৯:০০ - ১০:০০',
    startTime: '21:00',
    endTime: '22:00',
    color: '#10b981',
    subjects: [
      {
        id: 'sub-math',
        name: 'উচ্চতর গণিত (Higher Math)',
        code: 'HM-101',
        timeSlot: '৯:০০ PM - ১০:০০ PM',
        color: '#34d399',
        chapters: [
          {
            id: 'chap-straight-line',
            name: 'অধ্যায় ০৩: সরলরেখা',
            chapterNumber: '০৩',
            totalLectures: 15,
            lectures: createLectures(15, [], 5),
            notes: '১. ঢাল m = (y₂ - y₁) / (x₂ - x₁)\n২. দুটি সরলরেখার মধ্যবর্তী কোণ: tan θ = ± (m₁ - m₂) / (1 + m₁m₂)\n৩. লম্ব দূরত্ব: d = |ax₁ + by₁ + c| / √(a² + b²)',
            createdAt: new Date().toISOString(),
          }
        ],
      }
    ],
  },
  {
    id: 'session-3',
    name: 'সেশন ৩: রাত ১১:০০ - ১২:০০',
    startTime: '23:00',
    endTime: '23:59',
    color: '#f59e0b',
    subjects: [
      {
        id: 'sub-chem',
        name: 'রসায়ন (Chemistry)',
        code: 'CHEM-101',
        timeSlot: '১১:০০ PM - ১২:০০ AM',
        color: '#fbbf24',
        chapters: [
          {
            id: 'chap-qual-chem',
            name: 'অধ্যায় ০২: গুণগত রসায়ন',
            chapterNumber: '০২',
            totalLectures: 12,
            lectures: createLectures(12, [], 3),
            notes: '১. বোর পরমাণু মডেল ও রিডবার্গ ধ্রুবক: 1/λ = R_H (1/n₁² - 1/n₂²)\n২. কোয়ান্টাম সংখ্যা: n, l, m, s\n৩. দ্রাব্যতা গুণফল (Ksp) ও দ্রাব্যতা (S) গণনা',
            createdAt: new Date().toISOString(),
          }
        ],
      }
    ],
  },
];
