import { useAuth } from '@/context/AuthContext';
import { Navigate, Link } from 'react-router-dom';
import {
  Clock,
  Award,
  Flame,
  Star,
  ChevronRight,
  Award as Trophy,
  Code,
  Rocket,
  Lock,
  Terminal,
  PenTool,
  ArrowRight
} from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { mockProgressData as mockData } from '@/data/mockProgressData';

export default function ProgressPage() {
  const { isPublicAuthenticated } = useAuth();

  if (!isPublicAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="container mx-auto px-4 max-w-7xl py-8">
      {/* Breadcrumb & Title */}
      <div className="mb-8">
        <div className="flex items-center text-sm text-slate-500 mb-2">
          <Link to="/" className="hover:text-slate-900 transition-colors">Trang chủ</Link>
          <ChevronRight className="w-4 h-4 mx-1" />
          <span className="font-semibold text-slate-900">Tiến độ</span>
        </div>
        <h1 className="text-2xl font-bold text-slate-900">Tiến độ học tập của bạn</h1>
      </div>

      {/* 4 Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {/* Card 1 */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">Tổng thời gian học</p>
            <p className="text-2xl font-bold text-slate-900">{mockData.overview.total_hours} giờ</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-slate-50 flex items-center justify-center">
            <Clock className="w-6 h-6 text-slate-700" />
          </div>
        </div>

        {/* Card 2 */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">Khóa học hoàn thành</p>
            <p className="text-2xl font-bold text-slate-900">{mockData.overview.completed_courses} / {mockData.overview.total_enrolled_courses}</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-slate-50 flex items-center justify-center">
            <Award className="w-6 h-6 text-slate-700" />
          </div>
        </div>

        {/* Card 3 */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">Chuỗi ngày học</p>
            <p className="text-2xl font-bold text-orange-500">{mockData.overview.streak_days} ngày</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-orange-50 flex items-center justify-center">
            <Flame className="w-6 h-6 text-orange-500" />
          </div>
        </div>

        {/* Card 4 */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">Điểm tích lũy</p>
            <p className="text-2xl font-bold text-emerald-500">{(mockData.overview.total_xp / 1000).toFixed(1)}k XP</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center">
            <Star className="w-6 h-6 text-emerald-500" />
          </div>
        </div>
      </div>

      {/* Main Grid: Chart & Achievements */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
        {/* Chart */}
        <div className="lg:col-span-2 bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex flex-col">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-bold text-slate-900">Hoạt động trong tuần</h2>
            <button className="text-sm font-medium text-slate-500 hover:text-slate-900">Chi tiết</button>
          </div>
          <div className="flex-1 min-h-[250px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={mockData.weekly_chart}>
                <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} dy={10} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} dx={-10} tickFormatter={(val) => `${val}h`} />
                <Tooltip cursor={{ fill: '#f8fafc' }} contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
                <Bar dataKey="hours" fill="#6366f1" radius={[4, 4, 0, 0]} maxBarSize={40} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Achievements */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-bold text-slate-900">Thành tựu</h2>
            <Award className="w-5 h-5 text-slate-400" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            {mockData.achievements.map((item) => (
              <div key={item.id} className={`flex flex-col items-center p-4 text-center ${item.achieved ? '' : 'opacity-50 grayscale'}`}>
                <div className={`w-14 h-14 rounded-full flex items-center justify-center mb-3 ${item.bg}`}>
                  <item.icon className={`w-6 h-6 ${item.color}`} />
                </div>
                <h3 className="text-sm font-bold text-slate-900">{item.title}</h3>
                <p className="text-[11px] text-slate-500 mt-1">{item.description}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Ongoing Courses & Next Suggestion */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Ongoing Courses */}
        <div className="lg:col-span-2">
          <h2 className="text-lg font-bold text-slate-900 mb-4">Khóa học đang diễn ra</h2>
          <div className="space-y-4">
            {mockData.ongoing_courses.map((course) => (
              <div key={course.id} className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm flex items-center gap-4 hover:border-indigo-100 hover:shadow-md transition-all cursor-pointer">
                <div className="w-12 h-12 rounded-xl bg-slate-50 flex items-center justify-center shrink-0 border border-slate-100">
                  <course.icon className="w-5 h-5 text-slate-600" />
                </div>
                <div className="flex-1">
                  <h3 className="font-bold text-slate-900">{course.title}</h3>
                  <p className="text-xs text-slate-500 mt-1">Học lần cuối: {course.last_accessed}</p>
                </div>
                <div className="w-32 flex flex-col items-end gap-2 shrink-0">
                  <div className="flex items-center justify-between w-full text-xs">
                    <span className="text-slate-500">Tiến độ</span>
                    <span className="font-bold text-slate-900">{course.progress}%</span>
                  </div>
                  <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
                    <div className="h-full bg-slate-900 rounded-full" style={{ width: `${course.progress}%` }}></div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Next Suggestion Banner */}
        <div>
          <h2 className="text-lg font-bold text-slate-900 mb-4 invisible md:visible">Gợi ý</h2>
          <div className="rounded-2xl overflow-hidden relative shadow-sm h-[200px] sm:h-[240px] group cursor-pointer">
            <div className="absolute inset-0 bg-slate-900">
              {/* Decorative background gradient */}
              <div className="absolute inset-0 opacity-40 bg-[radial-gradient(ellipse_at_bottom_right,_var(--tw-gradient-stops))] from-indigo-500 via-slate-900 to-slate-900"></div>
            </div>

            <div className="absolute inset-0 p-6 flex flex-col justify-center">
              <span className="inline-block px-3 py-1 rounded-full bg-white/10 backdrop-blur-sm text-[11px] font-bold text-white uppercase tracking-wider w-fit mb-4">
                Gợi ý tiếp theo
              </span>
              <h3 className="text-xl font-bold text-white leading-tight mb-4 pr-12">
                {mockData.next_suggestion.title}
              </h3>
              <div className="flex items-center text-sm font-semibold text-white/90 group-hover:text-white transition-colors">
                Tiếp tục học
                <ArrowRight className="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
