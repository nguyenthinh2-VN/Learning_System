import { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Navigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Clock,
  Award,
  Flame,
  ChevronRight,
  BookOpen,
  AlertCircle
} from 'lucide-react';
import { ActivityCalendar } from 'react-activity-calendar';
import progressApi from '@/api/progressApi';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export default function ProgressPage() {
  const { t } = useTranslation();
  const { isPublicAuthenticated } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filterMandatory, setFilterMandatory] = useState(false);

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

  const generateFullYearHeatmap = (serverHeatmap) => {
    const dataMap = new Map();
    if (serverHeatmap) {
      serverHeatmap.forEach(item => {
        dataMap.set(item.date, item);
      });
    }

    const result = [];
    const today = new Date();
    const startDate = new Date();
    startDate.setFullYear(today.getFullYear() - 1); // 1 năm trước

    for (let d = new Date(startDate); d <= today; d.setDate(d.getDate() + 1)) {
      const dateStr = d.toISOString().split('T')[0];
      if (dataMap.has(dateStr)) {
        result.push(dataMap.get(dateStr));
      } else {
        result.push({ date: dateStr, count: 0, level: 0 });
      }
    }
    return result;
  };

  const heatmapData = generateFullYearHeatmap(heatmap);

  const customTheme = {
    light: ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39']
  };

  const formatDate = (isoString) => {
    if (!isoString) return t('ui.progress.not_started', 'Chưa học');
    const date = new Date(isoString);
    return date.toLocaleDateString('vi-VN');
  };

  const displayedCourses = ongoingCourses ? ongoingCourses.filter(c => !filterMandatory || c.isMandatory) : [];

  // Luôn lấy từ ongoingCourses gốc (không bị ảnh hưởng bởi filter)
  const unenrolledMandatoryCourses = ongoingCourses
    ? ongoingCourses.filter(c => c.isMandatory && c.statusMessage === "Chưa được đăng ký học do khóa bắt buộc")
    : [];


  return (
    <div className="container mx-auto px-4 max-w-7xl py-8">
      {/* Breadcrumb & Title */}
      <div className="mb-8">
        <div className="flex items-center text-sm text-slate-500 mb-2">
          <Link to="/" className="hover:text-slate-900 transition-colors">{t('ui.progress.home', 'Trang chủ')}</Link>
          <ChevronRight className="w-4 h-4 mx-1" />
          <span className="font-semibold text-slate-900">{t('ui.progress.progress', 'Tiến độ')}</span>
        </div>
        <h1 className="text-2xl font-bold text-slate-900">{t('ui.progress.title', 'Tiến độ học tập của bạn')}</h1>
      </div>

      {/* 3 Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        {/* Card 1 */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">{t('ui.progress.total_hours_label', 'Tổng thời gian học')}</p>
            <p className="text-2xl font-bold text-slate-900">{overview.totalHours} {t('ui.progress.hours', 'giờ')}</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-slate-50 flex items-center justify-center">
            <Clock className="w-6 h-6 text-slate-700" />
          </div>
        </div>

        {/* Card 2 */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">{t('ui.progress.completed_courses_label', 'Khóa học hoàn thành')}</p>
            <p className="text-2xl font-bold text-slate-900">{overview.completedCourses}</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-slate-50 flex items-center justify-center">
            <Award className="w-6 h-6 text-slate-700" />
          </div>
        </div>

        {/* Card 3 */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">{t('ui.progress.streak_days_label', 'Chuỗi ngày học')}</p>
            <p className="text-2xl font-bold text-orange-500">{overview.streakDays} {t('ui.progress.days', 'ngày')}</p>
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
            <h2 className="text-lg font-bold text-slate-900">{t('ui.progress.history_title', 'Lịch sử hoạt động')}</h2>
          </div>
          <div className="w-full overflow-x-auto pb-4 flex justify-center">
            <div className="min-w-fit">
              <ActivityCalendar
                data={heatmapData}
                theme={customTheme}
                labels={{
                  legend: { less: t('ui.progress.less', 'Ít'), more: t('ui.progress.more', 'Nhiều') },
                  months: t('ui.progress.months', { returnObjects: true, defaultValue: ['Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6', 'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'] }),
                  weekdays: t('ui.progress.weekdays', { returnObjects: true, defaultValue: ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'] }),
                  totalCount: `{{count}} ${t('ui.progress.activity_count', 'bài học trong khoảng thời gian')}`
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

      {/* Warning Unenrolled Mandatory Courses */}
      {unenrolledMandatoryCourses.length > 0 && (
        <div className="mb-8 bg-red-50 border border-red-200 rounded-2xl p-5 flex items-start gap-4">
          <AlertCircle className="w-6 h-6 text-red-500 shrink-0 mt-0.5" />
          <div>
            <h3 className="text-red-800 font-bold">{t('ui.progress.mandatory_warning_title', 'Bạn có khóa học bắt buộc chưa đăng ký')}</h3>
            <p className="text-sm text-red-600 mt-1 mb-3">{t('ui.progress.mandatory_warning_desc', 'Vui lòng đăng ký và hoàn thành các khóa học sau để đảm bảo tiến độ:')}</p>
            <ul className="space-y-2">
              {unenrolledMandatoryCourses.map(c => (
                <li key={c.id} className="text-sm text-red-700 flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-red-400"></span>
                  <span className="font-semibold">{c.title}</span>
                  <Link to={`/courses/${c.id}`} className="ml-2 px-3 py-1 bg-red-100 hover:bg-red-200 text-red-700 font-medium rounded-lg transition-colors text-xs">
                    {t('ui.progress.enroll_now', 'Đăng ký ngay')}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {/* Ongoing Courses */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-slate-900">{t('ui.progress.ongoing_courses_title', 'Khóa học đang diễn ra')}</h2>
          <Select
            value={filterMandatory ? 'mandatory' : 'all'}
            onValueChange={(v) => setFilterMandatory(v === 'mandatory')}
          >
            <SelectTrigger className="w-[180px] bg-white h-9">
              <SelectValue placeholder={t('ui.progress.filter_placeholder', 'Lọc khóa học')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t('ui.progress.filter_all', 'Tất cả khóa học')}</SelectItem>
              <SelectItem value="mandatory">{t('ui.progress.filter_mandatory', 'Chỉ khóa bắt buộc')}</SelectItem>
            </SelectContent>
          </Select>
        </div>
        {displayedCourses.length > 0 ? (
          <div className="space-y-4">
            {displayedCourses.map((course) => (
              <Link to={`/courses/${course.id}`} key={course.id} className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm flex items-center gap-4 hover:border-indigo-100 hover:shadow-md transition-all cursor-pointer block">
                <div className="w-24 h-16 rounded-xl bg-slate-100 flex items-center justify-center shrink-0 border border-slate-100 overflow-hidden">
                  {course.thumbnailUrl ? (
                    <img src={course.thumbnailUrl} alt={course.title} className="w-full h-full object-cover" />
                  ) : (
                    <BookOpen className="w-6 h-6 text-slate-400" />
                  )}
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <h3 className="font-bold text-slate-900 line-clamp-1">{course.title}</h3>
                    {course.isMandatory && (
                      <span className="shrink-0 inline-block px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700 text-[10px] font-bold uppercase tracking-wider">
                        {t('ui.progress.mandatory', 'Bắt buộc')}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-500">{t('ui.progress.last_accessed', 'Học lần cuối:')} {formatDate(course.lastAccessed)}</p>
                  {course.statusMessage && (
                    <p className={`text-xs mt-1 font-medium inline-block px-2 py-0.5 rounded border ${course.statusMessage.includes('Chưa được đăng ký')
                      ? 'text-red-600 bg-red-50 border-red-200'
                      : 'text-amber-600 bg-amber-50 border-amber-200'
                      }`}>
                      {course.statusMessage.includes('Chưa được đăng ký') ? t('ui.progress.not_enrolled_mandatory', 'Chưa đăng ký học khóa bắt buộc này') : course.statusMessage}
                    </p>
                  )}
                </div>
                <div className="w-24 md:w-32 flex flex-col items-end gap-2 shrink-0">
                  <div className="flex items-center justify-between w-full text-xs">
                    <span className="text-slate-500">{t('ui.progress.progress_label', 'Tiến độ')}</span>
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
            <p className="text-slate-500 text-sm">{t('ui.progress.no_ongoing_courses', 'Bạn chưa có khóa học nào đang diễn ra.')}</p>
            <Link to="/courses" className="text-indigo-600 font-medium text-sm mt-2 inline-block hover:underline">
              {t('ui.progress.explore_courses', 'Khám phá khóa học ngay')}
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
