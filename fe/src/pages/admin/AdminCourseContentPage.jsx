import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { useTranslation } from 'react-i18next';
import {
  adminGetCourseDetailApi,
  adminGetSectionsApi,
  adminCreateSectionApi,
  adminUpdateSectionApi,
  adminDeleteSectionApi,
  adminCreateLessonApi,
  adminUpdateLessonApi,
  adminDeleteLessonApi,
} from '@/api/adminApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import {
  ChevronDown, ChevronRight, Plus, Pencil, Trash2, Loader2,
  Check, X, ArrowLeft, BookOpen, Video, AlertCircle, GripVertical,
} from 'lucide-react';
import { getRoleTextClass } from '@/lib/roleColors';

// ─── Inline Edit Input ────────────────────────────────────
function InlineEdit({ value, onSave, onCancel, placeholder = '' }) {
  const [val, setVal] = useState(value);
  return (
    <div className="flex items-center gap-2 flex-1">
      <Input
        value={val}
        onChange={(e) => setVal(e.target.value)}
        placeholder={placeholder}
        className="h-7 text-sm"
        autoFocus
        onKeyDown={(e) => {
          if (e.key === 'Enter') onSave(val);
          if (e.key === 'Escape') onCancel();
        }}
      />
      <button onClick={() => onSave(val)} className="p-1 rounded hover:bg-emerald-500/20 text-emerald-400">
        <Check className="size-3.5" />
      </button>
      <button onClick={onCancel} className="p-1 rounded hover:bg-muted text-muted-foreground">
        <X className="size-3.5" />
      </button>
    </div>
  );
}

// ─── Lesson Row ───────────────────────────────────────────
function LessonRow({ lesson, courseId, sectionId, onUpdated, onDeleted }) {
  const { t } = useTranslation();
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState({ title: lesson.title, contentUrl: lesson.contentUrl, orderIndex: lesson.orderIndex });
  const [loading, setLoading] = useState(false);
  const [showUrlEdit, setShowUrlEdit] = useState(false);

  const handleSaveTitle = async (newTitle) => {
    if (!newTitle.trim()) { setEditing(false); return; }
    setLoading(true);
    try {
      await adminUpdateLessonApi(courseId, sectionId, lesson.id, {
        title: newTitle.trim(),
        contentUrl: lesson.contentUrl,
        orderIndex: lesson.orderIndex,
      });
      onUpdated();
    } catch { /* ignore */ }
    finally { setLoading(false); setEditing(false); }
  };

  const handleSaveUrl = async (newUrl) => {
    if (!newUrl.trim()) { setShowUrlEdit(false); return; }
    setLoading(true);
    try {
      await adminUpdateLessonApi(courseId, sectionId, lesson.id, {
        title: lesson.title,
        contentUrl: newUrl.trim(),
        orderIndex: lesson.orderIndex,
      });
      onUpdated();
    } catch { /* ignore */ }
    finally { setLoading(false); setShowUrlEdit(false); }
  };

  const handleDelete = async () => {
    if (!confirm(t('ui.admin_course_content.delete_lesson_confirm').replace('{title}', lesson.title))) return;
    setLoading(true);
    try {
      await adminDeleteLessonApi(courseId, sectionId, lesson.id);
      onDeleted(lesson.id);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  return (
    <div className="flex items-start gap-2 px-3 py-2 rounded-lg hover:bg-muted/30 group">
      <GripVertical className="size-3.5 text-muted-foreground/40 mt-1 shrink-0" />
      <Video className="size-3.5 text-muted-foreground mt-1 shrink-0" />
      <div className="flex-1 min-w-0">
        {editing ? (
          <InlineEdit value={lesson.title} onSave={handleSaveTitle} onCancel={() => setEditing(false)} placeholder={t('ui.admin_course_content.lesson_title_placeholder')} />
        ) : (
          <p className="text-sm leading-tight">{lesson.title}</p>
        )}
        {showUrlEdit ? (
          <div className="mt-1">
            <InlineEdit value={lesson.contentUrl || ''} onSave={handleSaveUrl} onCancel={() => setShowUrlEdit(false)} placeholder={t('ui.admin_course_content.url_placeholder')} />
          </div>
        ) : (
          <p className="text-xs text-muted-foreground mt-0.5 truncate max-w-xs">
            {lesson.contentUrl || <span className="italic">{t('ui.admin_course_content.no_url')}</span>}
          </p>
        )}
      </div>
      <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
        {loading ? (
          <Loader2 className="size-3.5 animate-spin text-muted-foreground" />
        ) : (
          <>
            <button onClick={() => setEditing(true)} className="p-1 rounded hover:bg-muted text-muted-foreground hover:text-foreground" title={t('ui.admin_course_content.edit_title')}>
              <Pencil className="size-3" />
            </button>
            <button onClick={() => setShowUrlEdit(true)} className="p-1 rounded hover:bg-muted text-muted-foreground hover:text-foreground" title={t('ui.admin_course_content.edit_url')}>
              <Video className="size-3" />
            </button>
            <button onClick={handleDelete} className="p-1 rounded hover:bg-destructive/20 text-muted-foreground hover:text-destructive" title={t('ui.admin_course_content.delete')}>
              <Trash2 className="size-3" />
            </button>
          </>
        )}
      </div>
    </div>
  );
}

// ─── Add Lesson Form ──────────────────────────────────────
function AddLessonForm({ courseId, sectionId, nextOrder, onAdded, onCancel }) {
  const { t } = useTranslation();
  const [form, setForm] = useState({ title: '', contentUrl: '', orderIndex: nextOrder });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title.trim()) { setError(t('ui.admin_course_content.err_empty_lesson_title')); return; }
    if (!form.contentUrl.trim()) { setError(t('ui.admin_course_content.err_empty_url')); return; }
    setLoading(true);
    try {
      await adminCreateLessonApi(courseId, sectionId, {
        title: form.title.trim(),
        contentUrl: form.contentUrl.trim(),
        orderIndex: parseInt(form.orderIndex) || nextOrder,
      });
      onAdded();
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_course_content.err_add_lesson_fail'));
    } finally { setLoading(false); }
  };

  return (
    <form onSubmit={handleSubmit} className="mt-2 p-3 rounded-lg border border-border bg-muted/20 space-y-2">
      {error && <p className="text-xs text-destructive">{error}</p>}
      <div className="grid grid-cols-2 gap-2">
        <Input
          value={form.title}
          onChange={(e) => setForm((p) => ({ ...p, title: e.target.value }))}
          placeholder={t('ui.admin_course_content.lesson_title_req')}
          className="h-8 text-sm"
          autoFocus
        />
        <Input
          value={form.orderIndex}
          onChange={(e) => setForm((p) => ({ ...p, orderIndex: e.target.value }))}
          type="number"
          placeholder={t('ui.admin_course_content.order')}
          className="h-8 text-sm"
          min={0}
        />
      </div>
      <Input
        value={form.contentUrl}
        onChange={(e) => setForm((p) => ({ ...p, contentUrl: e.target.value }))}
        placeholder={t('ui.admin_course_content.url_req')}
        className="h-8 text-sm"
      />
      <div className="flex gap-2">
        <Button type="submit" size="sm" disabled={loading} className="h-7 text-xs">
          {loading && <Loader2 className="size-3 animate-spin mr-1" />}Thêm bài
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={onCancel} className="h-7 text-xs">{t('ui.admin_course_content.cancel')}</Button>
      </div>
    </form>
  );
}

// ─── Section Card ─────────────────────────────────────────
function SectionCard({ section, courseId, onUpdated, onDeleted }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(true);
  const [editingTitle, setEditingTitle] = useState(false);
  const [addingLesson, setAddingLesson] = useState(false);
  const [loading, setLoading] = useState(false);
  const [lessons, setLessons] = useState(section.lessons || []);

  const handleSaveTitle = async (newTitle) => {
    if (!newTitle.trim()) { setEditingTitle(false); return; }
    setLoading(true);
    try {
      await adminUpdateSectionApi(courseId, section.id, {
        title: newTitle.trim(),
        orderIndex: section.orderIndex,
      });
      onUpdated();
    } catch { /* ignore */ }
    finally { setLoading(false); setEditingTitle(false); }
  };

  const handleDelete = async () => {
    if (!confirm(t('ui.admin_course_content.delete_section_confirm').replace('{title}', section.title).replace('{count}', lessons.length))) return;
    setLoading(true);
    try {
      await adminDeleteSectionApi(courseId, section.id);
      onDeleted(section.id);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleLessonDeleted = (lessonId) => {
    setLessons((prev) => prev.filter((l) => l.id !== lessonId));
  };

  return (
    <div className="rounded-xl border border-border bg-card overflow-hidden">
      {/* Section header */}
      <div className="flex items-center gap-2 px-4 py-3 bg-muted/30">
        <button onClick={() => setOpen((v) => !v)} className="text-muted-foreground hover:text-foreground">
          {open ? <ChevronDown className="size-4" /> : <ChevronRight className="size-4" />}
        </button>
        <BookOpen className="size-4 text-muted-foreground shrink-0" />

        {editingTitle ? (
          <InlineEdit value={section.title} onSave={handleSaveTitle} onCancel={() => setEditingTitle(false)} placeholder={t('ui.admin_course_content.section_name_placeholder')} />
        ) : (
          <span className="flex-1 text-sm font-medium">{section.title}</span>
        )}

        <span className="text-xs text-muted-foreground shrink-0">{lessons.length} {t('ui.admin_course_content.lessons_count')}</span>

        {loading ? (
          <Loader2 className="size-4 animate-spin text-muted-foreground" />
        ) : (
          <div className="flex items-center gap-1">
            <button onClick={() => setEditingTitle(true)} className="p-1 rounded hover:bg-muted text-muted-foreground hover:text-foreground" title={t('ui.admin_course_content.edit_section_title')}>
              <Pencil className="size-3.5" />
            </button>
            <button onClick={handleDelete} className="p-1 rounded hover:bg-destructive/20 text-muted-foreground hover:text-destructive" title={t('ui.admin_course_content.delete_section')}>
              <Trash2 className="size-3.5" />
            </button>
          </div>
        )}
      </div>

      {/* Lessons */}
      {open && (
        <div className="px-2 py-2 space-y-0.5">
          {lessons.length === 0 && !addingLesson && (
            <p className="text-xs text-muted-foreground text-center py-3">{t('ui.admin_course_content.no_lessons_yet')}</p>
          )}
          {lessons.map((lesson) => (
            <LessonRow
              key={lesson.id}
              lesson={lesson}
              courseId={courseId}
              sectionId={section.id}
              onUpdated={onUpdated}
              onDeleted={handleLessonDeleted}
            />
          ))}

          {addingLesson ? (
            <AddLessonForm
              courseId={courseId}
              sectionId={section.id}
              nextOrder={lessons.length + 1}
              onAdded={() => { setAddingLesson(false); onUpdated(); }}
              onCancel={() => setAddingLesson(false)}
            />
          ) : (
            <button
              onClick={() => setAddingLesson(true)}
              className="flex items-center gap-2 w-full px-3 py-2 text-xs text-muted-foreground hover:text-foreground hover:bg-muted/30 rounded-lg transition-colors"
            >
              <Plus className="size-3.5" />
              Thêm bài giảng
            </button>
          )}
        </div>
      )}
    </div>
  );
}

// ─── Add Section Form ─────────────────────────────────────
function AddSectionForm({ courseId, nextOrder, onAdded, onCancel }) {
  const { t } = useTranslation();
  const [form, setForm] = useState({ title: '', orderIndex: nextOrder });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title.trim()) { setError(t('ui.admin_course_content.err_empty_section_title')); return; }
    setLoading(true);
    try {
      await adminCreateSectionApi(courseId, {
        title: form.title.trim(),
        orderIndex: parseInt(form.orderIndex) || nextOrder,
      });
      onAdded();
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_course_content.err_add_section_fail'));
    } finally { setLoading(false); }
  };

  return (
    <form onSubmit={handleSubmit} className="rounded-xl border border-dashed border-border p-4 space-y-3">
      <p className="text-sm font-medium">{t('ui.admin_course_content.add_new_section')}</p>
      {error && (
        <div className="flex items-center gap-2 text-xs text-destructive">
          <AlertCircle className="size-3.5" />{error}
        </div>
      )}
      <div className="grid grid-cols-3 gap-2">
        <div className="col-span-2 space-y-1">
          <Label className="text-xs">{t('ui.admin_course_content.section_name_req')}</Label>
          <Input
            value={form.title}
            onChange={(e) => setForm((p) => ({ ...p, title: e.target.value }))}
            placeholder={t('ui.admin_course_content.section_name_example')}
            className="h-8 text-sm"
            autoFocus
          />
        </div>
        <div className="space-y-1">
          <Label className="text-xs">{t('ui.admin_course_content.order')}</Label>
          <Input
            value={form.orderIndex}
            onChange={(e) => setForm((p) => ({ ...p, orderIndex: e.target.value }))}
            type="number"
            className="h-8 text-sm"
            min={0}
          />
        </div>
      </div>
      <div className="flex gap-2">
        <Button type="submit" size="sm" disabled={loading}>
          {loading && <Loader2 className="size-4 animate-spin mr-2" />}Thêm chương
        </Button>
        <Button type="button" size="sm" variant="outline" onClick={onCancel}>{t('ui.admin_course_content.cancel')}</Button>
      </div>
    </form>
  );
}

// ─── Main Page ────────────────────────────────────────────
export default function AdminCourseContentPage() {
  const { t } = useTranslation();
  const { id: courseId } = useParams();
  const { adminUser } = useAuth();
  const [course, setCourse] = useState(null);
  const [sections, setSections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [addingSection, setAddingSection] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [courseRes, sectionsRes] = await Promise.all([
        adminGetCourseDetailApi(courseId),
        adminGetSectionsApi(courseId),
      ]);
      setCourse(courseRes.data.data);
      const sectionsData = sectionsRes.data.data;
      setSections(Array.isArray(sectionsData) ? sectionsData : []);
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_course_content.err_load_content_fail'));
    } finally { setLoading(false); }
  }, [courseId]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleSectionDeleted = (sectionId) => {
    setSections((prev) => prev.filter((s) => s.id !== sectionId));
  };

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b border-border bg-background/80 backdrop-blur px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <Link to="/admin/courses" className="flex items-center gap-1.5 text-muted-foreground hover:text-foreground transition-colors text-sm">
          <ArrowLeft className="size-3.5" />
          Khóa học
        </Link>
        <span className="text-muted-foreground">/</span>
        <span className="text-sm font-medium truncate max-w-xs">{course?.title || `Course #${courseId}`}</span>
        <span className={`text-xs font-medium ml-auto ${getRoleTextClass(adminUser?.role)}`}>{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-4xl mx-auto space-y-6">
        {/* Page title */}
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">{t('ui.admin_course_content.manage_content')}</h1>
            {course && (
              <p className="text-sm text-muted-foreground mt-1">
                {t('ui.admin_course_content.course_summary').replace('{title}', course.title).replace('{sections}', sections.length).replace('{lessons}', sections.reduce((acc, s) => acc + (s.lessons?.length || 0), 0))}
                
              </p>
            )}
          </div>
          <Button size="sm" onClick={() => setAddingSection(true)} disabled={addingSection}>
            <Plus className="size-4 mr-2" />Thêm chương
          </Button>
        </div>

        {error && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
            <AlertCircle className="size-4" /><span>{error}</span>
          </div>
        )}

        {loading ? (
          <div className="py-20 flex items-center justify-center">
            <Loader2 className="size-6 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <div className="space-y-3">
            {sections.length === 0 && !addingSection && (
              <div className="py-16 text-center border border-dashed border-border rounded-xl text-sm text-muted-foreground">
                <BookOpen className="size-8 mx-auto mb-2 opacity-20" />
                Chưa có chương nào. Nhấn "Thêm chương" để bắt đầu.
              </div>
            )}

            {sections.map((section) => (
              <SectionCard
                key={section.id}
                section={section}
                courseId={courseId}
                onUpdated={fetchData}
                onDeleted={handleSectionDeleted}
              />
            ))}

            {addingSection && (
              <AddSectionForm
                courseId={courseId}
                nextOrder={sections.length + 1}
                onAdded={() => { setAddingSection(false); fetchData(); }}
                onCancel={() => setAddingSection(false)}
              />
            )}
          </div>
        )}
      </div>
    </div>
  );
}
