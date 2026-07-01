import React, { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Navigate, Link } from 'react-router-dom';
import {
  Clock,
  Award,
  Flame,
  ChevronRight,
  ArrowRight,
  BookOpen
} from 'lucide-react';
import { ActivityCalendar } from 'react-activity-calendar';
import progressApi from '@/api/progressApi';

export default function ProgressPage() {
  const { isPublicAuthenticated } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (isPublicAuthenticated) {
      progressApi.getProgress()
        .then(res => {
          setData(res.data);
          setLoading(false);
        })
        .catch(err => {
          console.error('Failed to fetch progress', err);
          setLoading(false);
        });
    }
  }, [isPublicAuthenticated]);

  if (!isPublicAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-slate-900"></div>
      </div>
    );
  }

  if (!data) return null;

  const { overview, heatmap, ongoingCourses } = data;

  const heatmapData = heatmap && heatmap.length > 0
    ? heatmap
    : [{ date: new Date().toISOString().split('T')[0], count: 0, level: 0 }];

  const customTheme = {
    light: ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39']
  };

  const formatDate = (isoString) => {
    if (!isoString) return 'Chưa học';
    const date = new Date(isoString);
    return date.toLocaleDateString('vi-VN');
  };

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

      {/* 3 Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        {/* Card 1 */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">Tổng thời gian học</p>
            <p className="text-2xl font-bold text-slate-900">{overview.totalHours} giờ</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-slate-50 flex items-center justify-center">
            <Clock className="w-6 h-6 text-slate-700" />
          </div>
        </div>

        {/* Card 2 */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">Khóa học hoàn thành</p>
            <p className="text-2xl font-bold text-slate-900">{overview.completedCourses}</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-slate-50 flex items-center justify-center">
            <Award className="w-6 h-6 text-slate-700" />
          </div>
        </div>

        {/* Card 3 */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">Chuỗi ngày học</p>
            <p className="text-2xl font-bold text-orange-500">{overview.streakDays} ngày</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-orange-50 flex items-center justify-center">
            <Flame className="w-6 h-6 text-orange-500" />
          </div>
        </div>
      </div>

      {/* Main Section: Heatmap */}
      <div className="mb-12">
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex flex-col items-center">
          <div className="flex items-center justify-between w-full mb-6">
            <h2 className="text-lg font-bold text-slate-900">Lịch sử hoạt động</h2>
          </div>
          <div className="w-full overflow-x-auto pb-4 flex justify-center">
            <div className="min-w-fit">
              <ActivityCalendar
                data={heatmapData}
                theme={customTheme}
                labels={{
                  legend: { less: 'Ít', more: 'Nhiều' },
                  months: ['Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6', 'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'],
                  weekdays: ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'],
                  totalCount: '{{count}} bài học trong khoảng thời gian'
                }}
                showWeekdayLabels
                blockSize={14}
                blockRadius={4}
                blockMargin={4}
                fontSize={14}
              />
            </div>
          </div>
        </div>
      </div>

      {/* Ongoing Courses & Next Suggestion */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Ongoing Courses */}
        <div className="lg:col-span-2">
          <h2 className="text-lg font-bold text-slate-900 mb-4">Khóa học đang diễn ra</h2>
          {ongoingCourses && ongoingCourses.length > 0 ? (
            <div className="space-y-4">
              {ongoingCourses.map((course) => (
                <Link to={`/courses/${course.id}`} key={course.id} className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm flex items-center gap-4 hover:border-indigo-100 hover:shadow-md transition-all cursor-pointer block">
                  <div className="w-24 h-16 rounded-xl bg-slate-100 flex items-center justify-center shrink-0 border border-slate-100 overflow-hidden">
                    {course.thumbnailUrl ? (
                      <img src={course.thumbnailUrl} alt={course.title} className="w-full h-full object-cover" />
                    ) : (
                      <BookOpen className="w-6 h-6 text-slate-400" />
                    )}
                  </div>
                  <div className="flex-1">
                    <h3 className="font-bold text-slate-900 line-clamp-1">{course.title}</h3>
                    <p className="text-xs text-slate-500 mt-1">Học lần cuối: {formatDate(course.lastAccessed)}</p>
                  </div>
                  <div className="w-24 md:w-32 flex flex-col items-end gap-2 shrink-0">
                    <div className="flex items-center justify-between w-full text-xs">
                      <span className="text-slate-500">Tiến độ</span>
                      <span className="font-bold text-slate-900">{course.progressPercentage}%</span>
                    </div>
                    <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
                      <div className="h-full bg-indigo-600 rounded-full" style={{ width: `${course.progressPercentage}%` }}></div>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div className="bg-slate-50 border border-slate-100 border-dashed rounded-2xl p-8 text-center">
              <p className="text-slate-500 text-sm">Bạn chưa có khóa học nào đang diễn ra.</p>
              <Link to="/courses" className="text-indigo-600 font-medium text-sm mt-2 inline-block hover:underline">
                Khám phá khóa học ngay
              </Link>
            </div>
          )}
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
              <h3 className="text-xl font-bold text-white leading-tight mb-4 pr-12 line-clamp-2">
                Hoàn thiện kiến thức với Spring Boot Cơ Bản
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
