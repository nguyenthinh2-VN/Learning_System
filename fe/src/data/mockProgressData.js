import { Trophy, Code, Rocket, Lock, Terminal, PenTool } from 'lucide-react';

export const mockProgressData = {
  overview: {
    total_hours: 124,
    completed_courses: 12,
    total_enrolled_courses: 15,
    streak_days: 15,
    total_xp: 2400
  },
  weekly_chart: [
    { day: "T2", hours: 2 },
    { day: "T3", hours: 3.5 },
    { day: "T4", hours: 1 },
    { day: "T5", hours: 4 },
    { day: "T6", hours: 2 },
    { day: "T7", hours: 0 },
    { day: "CN", hours: 5 }
  ],
  achievements: [
    { id: 1, title: "Học giả chăm chỉ", description: "Đạt streak 7 ngày", icon: Trophy, color: "text-amber-500", bg: "bg-amber-100", achieved: true },
    { id: 2, title: "Vua Code", description: "Hoàn thành 50 bài tập", icon: Code, color: "text-indigo-500", bg: "bg-indigo-100", achieved: true },
    { id: 3, title: "Người mới năng nổ", description: "Hoàn thành khóa đầu tiên", icon: Rocket, color: "text-emerald-500", bg: "bg-emerald-100", achieved: true },
    { id: 4, title: "Bậc thầy API", description: "Cần 1 khóa API", icon: Lock, color: "text-slate-400", bg: "bg-slate-100", achieved: false }
  ],
  ongoing_courses: [
    { id: 1, title: "Spring Boot Clean Architecture", progress: 78, last_accessed: "2 giờ trước", icon: Code },
    { id: 2, title: "React & Spring Boot Fullstack", progress: 45, last_accessed: "Hôm qua", icon: Terminal },
    { id: 3, title: "Tailwind CSS cao cấp", progress: 10, last_accessed: "3 ngày trước", icon: PenTool }
  ],
  next_suggestion: {
    title: "Hoàn thiện dự án Spring Boot"
  }
};
