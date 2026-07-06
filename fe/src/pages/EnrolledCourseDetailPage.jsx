import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useCourseDetailStore from '@/store/useCourseDetailStore';
import { getCourseProgressApi, markLessonCompleteApi, unmarkLessonCompleteApi } from '@/api/course';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  ArrowLeft, ArrowRight, PlayCircle, BookOpen,
  ChevronDown, CheckCircle2, Circle, Loader2,
} from 'lucide-react';
import SectionTestView from '@/components/course/SectionTestView';

function VideoPlayer({ url }) {
  if (!url) {
    return (
      <div className="aspect-video bg-muted flex flex-col items-center justify-center rounded-xl gap-3">
        <PlayCircle className="size-12 text-muted-foreground/30" />
        <p className="text-sm text-muted-foreground">Không có video</p>
      </div>
    );
  }

  // Phát hiện loại URL để render đúng player
  const isDirectVideo = /\.(mp4|webm|ogg)(\?|$)/i.test(url);

  if (isDirectVideo) {
    return (
      <video
        key={url}
        className="w-full aspect-video rounded-xl bg-black"
        controls
        src={url}
      />
    );
  }

  // YouTube, Vimeo, hoặc iframe embed khác
  let embedUrl = url;
  // Chuyển YouTube watch URL → embed URL
  const ytMatch = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([^&\s]+)/);
  if (ytMatch) {
    embedUrl = `https://www.youtube.com/embed/${ytMatch[1]}?autoplay=0&rel=0`;
  }
  // Vimeo
  const vimeoMatch = url.match(/vimeo\.com\/(\d+)/);
  if (vimeoMatch) {
    embedUrl = `https://player.vimeo.com/video/${vimeoMatch[1]}`;
  }

  return (
    <iframe
      key={url}
      src={embedUrl}
      className="w-full aspect-video rounded-xl bg-black"
      allowFullScreen
      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
      title="Lesson video"
    />
  );
}

export default function EnrolledCourseDetailPage() {
  const { t } = useTranslation();
  const { courseId } = useParams();
  const navigate = useNavigate();
  const { currentCourse, loading, error, fetchEnrolledDetail } = useCourseDetailStore();

  const [currentLessonIndex, setCurrentLessonIndex] = useState(0);
  const [openSections, setOpenSections] = useState({});

  // Tiến độ học
  const [completedIds, setCompletedIds] = useState(new Set());
  const [progressPercent, setProgressPercent] = useState(0);
  const [markBusy, setMarkBusy] = useState(false);

  const loadProgress = useCallback(async (cid) => {
    try {
      const res = await getCourseProgressApi(cid);
      const data = res.data.data;
      setCompletedIds(new Set(data.completedLessonIds || []));
      setProgressPercent(data.progressPercent ?? 0);
    } catch {
      // tiến độ không tải được không nên chặn việc học — bỏ qua lỗi mềm
    }
  }, []);

  useEffect(() => {
    if (courseId) {
      fetchEnrolledDetail(Number(courseId));
      setCurrentLessonIndex(0);
      loadProgress(Number(courseId));
    }
  }, [courseId, fetchEnrolledDetail, loadProgress]);

  // Flatten tất cả lessons từ tất cả sections
  const sortedSections = currentCourse?.sections
    ? [...currentCourse.sections].sort((a, b) => a.orderIndex - b.orderIndex)
    : [];

  const flatLessons = sortedSections.flatMap((section) => {
    const lessons = (section.lessons || [])
      .slice()
      .sort((a, b) => a.orderIndex - b.orderIndex)
      .map((lesson) => ({ ...lesson, sectionTitle: section.title, isTest: false }));
    
    if (section.hasTest) {
      lessons.push({
        id: `test-${section.id}`,
        sectionId: section.id,
        title: 'Bài Kiểm Tra Chương',
        sectionTitle: section.title,
        isTest: true,
        orderIndex: 9999
      });
    }
    return lessons;
  });

  const currentLesson = flatLessons[currentLessonIndex] ?? null;
  const totalLessons = flatLessons.length;
  const isCurrentDone = currentLesson != null && completedIds.has(currentLesson.id);

  const toggleComplete = useCallback(async () => {
    if (!currentLesson?.id || markBusy) return;
    const cid = Number(courseId);
    const lessonId = currentLesson.id;
    const wasDone = completedIds.has(lessonId);
    setMarkBusy(true);
    try {
      if (wasDone) {
        await unmarkLessonCompleteApi(cid, lessonId);
      } else {
        await markLessonCompleteApi(cid, lessonId);
      }
      await loadProgress(cid);
    } catch {
      // bỏ qua — giữ nguyên trạng thái nếu lỗi
    } finally {
      setMarkBusy(false);
    }
  }, [courseId, currentLesson, completedIds, markBusy, loadProgress]);

  // Đánh dấu hoàn thành bài hiện tại (chỉ mark, không bỏ đánh dấu) — dùng khi bấm "Bài tiếp"
  const markCurrentDone = useCallback(async () => {
    if (!currentLesson?.id) return;
    const lessonId = currentLesson.id;
    if (completedIds.has(lessonId)) return; // đã xong thì thôi
    const cid = Number(courseId);
    // Optimistic: cập nhật ngay để UI mượt, rồi đồng bộ lại từ server
    setCompletedIds((prev) => new Set(prev).add(lessonId));
    try {
      await markLessonCompleteApi(cid, lessonId);
      await loadProgress(cid);
    } catch {
      // lỗi → reload lại trạng thái thật
      loadProgress(cid);
    }
  }, [courseId, currentLesson, completedIds, loadProgress]);

  const goToLesson = useCallback((idx) => {
    if (idx >= 0 && idx < totalLessons) {
      setCurrentLessonIndex(idx);
      // Tự động mở section chứa bài đang học
      let count = 0;
      sortedSections.forEach((section, sIdx) => {
        const len = section.lessons?.length || 0;
        if (idx >= count && idx < count + len) {
          setOpenSections((prev) => ({ ...prev, [sIdx]: true }));
        }
        count += len;
      });
    }
  }, [totalLessons, sortedSections]);

  // Mở section đầu tiên khi data load
  useEffect(() => {
    if (sortedSections.length > 0) {
      setOpenSections({ 0: true });
    }
  }, [currentCourse?.id]);

  const toggleSection = (idx) =>
    setOpenSections((prev) => ({ ...prev, [idx]: !prev[idx] }));

  // Tính index của lesson trong flat list để highlight sidebar
  let lessonFlatCounter = 0;

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-6 py-10 space-y-6">
        <Skeleton className="h-6 w-48" />
        <Skeleton className="aspect-video w-full rounded-xl" />
        <div className="grid grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => <Skeleton key={i} className="h-24 rounded-lg" />)}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-xl mx-auto px-6 py-20 text-center space-y-3">
        <BookOpen className="size-12 mx-auto text-muted-foreground/30" />
        <p className="text-sm text-muted-foreground">{error}</p>
        <Button variant="outline" size="sm" onClick={() => navigate('/my-courses')}>
          <ArrowLeft className="size-4 mr-1" /> Về khóa học của tôi
        </Button>
      </div>
    );
  }

  if (!currentCourse) return null;

  return (
    <div className="max-w-6xl mx-auto px-4 py-6">
      {/* ── Back ─────────────────────────────────────────────────────── */}
      <button
        onClick={() => navigate('/my-courses')}
        className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors mb-5"
      >
        <ArrowLeft className="size-4" /> Khóa học của tôi
      </button>

      <h1 className="text-xl font-bold mb-3 leading-tight">{currentCourse.title}</h1>

      {/* Progress bar */}
      <div className="mb-6 flex items-center gap-3">
        <div className="flex-1 h-2 rounded-full bg-muted overflow-hidden">
          <div
            className="h-full bg-emerald-500 transition-all duration-300"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
        <span className="text-xs font-medium text-muted-foreground tabular-nums shrink-0">
          {progressPercent}% hoàn thành
        </span>
      </div>

      {/* ── Main layout: [Video + nav] | [Sidebar] ──────────────────── */}
      <div className="grid lg:grid-cols-3 gap-6">

        {/* ── Left: Video player or Test + controls ──────────────────────── */}
        <div className="lg:col-span-2 space-y-4">
          {currentLesson?.isTest ? (
            <SectionTestView sectionId={currentLesson.sectionId} onTestPassed={() => {
              markCurrentDone(); 
              loadProgress(Number(courseId));
            }} />
          ) : (
            <VideoPlayer url={currentLesson?.contentUrl} />
          )}

          {/* Lesson title + prev/next */}
          <div className="border rounded-xl p-4 space-y-3">
            <div>
              <p className="text-xs text-muted-foreground mb-0.5">
                {currentLesson?.sectionTitle}
              </p>
              <h2 className="font-semibold text-base leading-snug">
                {currentLesson?.title ?? 'Chưa chọn bài học'}
              </h2>
            </div>

            {/* Đánh dấu hoàn thành */}
            {currentLesson && !currentLesson.isTest && (
              <Button
                size="sm"
                onClick={toggleComplete}
                disabled={markBusy}
                className={`gap-1.5 w-full sm:w-auto border ${
                  isCurrentDone
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100'
                    : 'bg-emerald-600 text-white border-emerald-600 hover:bg-emerald-700'
                }`}
              >
                {markBusy ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : isCurrentDone ? (
                  <CheckCircle2 className="size-4" />
                ) : (
                  <Circle className="size-4" />
                )}
                {isCurrentDone ? 'Đã hoàn thành — bỏ đánh dấu' : 'Đánh dấu đã học'}
              </Button>
            )}

            <div className="flex items-center justify-between">
              <Button
                variant="outline"
                size="sm"
                disabled={currentLessonIndex === 0}
                onClick={() => goToLesson(currentLessonIndex - 1)}
                className="gap-1.5"
              >
                <ArrowLeft className="size-4" /> Bài trước
              </Button>

              <span className="text-sm text-muted-foreground font-medium tabular-nums">
                {totalLessons > 0 ? `${currentLessonIndex + 1} / ${totalLessons}` : '—'}
              </span>

              <Button
                variant="outline"
                size="sm"
                disabled={currentLessonIndex >= totalLessons - 1}
                onClick={() => { markCurrentDone(); goToLesson(currentLessonIndex + 1); }}
                className="gap-1.5"
              >
                Bài tiếp <ArrowRight className="size-4" />
              </Button>
            </div>
          </div>
        </div>

        {/* ── Right: Sidebar danh sách sections + lessons ─────────── */}
        <div className="border rounded-xl overflow-hidden h-fit max-h-[70vh] flex flex-col">
          <div className="px-4 py-3 border-b bg-muted/30 shrink-0">
            <p className="font-semibold text-sm">{t('ui.course.course_content')}</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              {sortedSections.length} chương · {totalLessons} bài
            </p>
          </div>

          <div className="overflow-y-auto divide-y">
            {sortedSections.map((section, sIdx) => {
              const sectionLessons = (section.lessons || [])
                .slice()
                .sort((a, b) => a.orderIndex - b.orderIndex)
                .map(l => ({ ...l, isTest: false }));
              
              if (section.hasTest) {
                sectionLessons.push({
                  id: `test-${section.id}`,
                  sectionId: section.id,
                  title: 'Bài Kiểm Tra Chương',
                  isTest: true,
                });
              }

              const sectionStartIdx = lessonFlatCounter;
              lessonFlatCounter += sectionLessons.length;
              const isOpen = !!openSections[sIdx];

              return (
                <div key={sIdx}>
                  {/* Section header */}
                  <button
                    className="w-full flex items-center justify-between px-4 py-3 hover:bg-muted/40 transition-colors text-left bg-muted/10"
                    onClick={() => toggleSection(sIdx)}
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <span className="font-medium text-xs truncate">{section.title}</span>
                      <Badge variant="outline" className="text-[9px] shrink-0 font-normal">
                        {sectionLessons.length}
                      </Badge>
                    </div>
                    <ChevronDown
                      className="size-3.5 text-muted-foreground shrink-0 ml-1 transition-transform duration-200"
                      style={{ transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)' }}
                    />
                  </button>

                  {/* Lessons */}
                  {isOpen && (
                    <div className="divide-y">
                      {sectionLessons.length === 0 ? (
                        <p className="px-5 py-2.5 text-xs text-muted-foreground">Chưa có bài.</p>
                      ) : (
                        sectionLessons.map((lesson, lIdx) => {
                          const flatIdx = sectionStartIdx + lIdx;
                          const isActive = flatIdx === currentLessonIndex;
                          const isDone = completedIds.has(lesson.id);

                          return (
                            <button
                              key={lesson.id ?? lIdx}
                              onClick={() => goToLesson(flatIdx)}
                              className={`w-full flex items-center gap-2.5 px-5 py-2.5 text-left transition-colors ${
                                isActive
                                  ? 'bg-foreground text-background'
                                  : 'hover:bg-muted/50'
                              }`}
                            >
                                {isDone ? (
                                  <CheckCircle2 className={`size-3.5 shrink-0 ${isActive ? 'text-emerald-300' : 'text-emerald-500'}`} />
                                ) : isActive ? (
                                  lesson.isTest ? <BookOpen className="size-3.5 shrink-0" /> : <PlayCircle className="size-3.5 shrink-0" />
                                ) : (
                                  lesson.isTest ? <BookOpen className="size-3.5 shrink-0 text-muted-foreground/40" /> : <PlayCircle className="size-3.5 shrink-0 text-muted-foreground/40" />
                                )}
                                <span className={`text-xs leading-snug line-clamp-2 ${
                                  isActive ? '' : 'text-foreground'
                                }`}>
                                {lesson.title}
                              </span>
                            </button>
                          );
                        })
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
