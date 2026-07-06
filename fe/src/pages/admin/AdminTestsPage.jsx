import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { getAdminCoursesApi, getInstructorCoursesApi, adminGetSectionsApi, adminUpdateSectionTestApi, adminGetSectionTestApi } from '@/api/adminApi';
import { useAuth } from '@/context/AuthContext';
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardDescription,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Loader2, Plus, Trash2, Save, FileQuestion, ChevronDown } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from 'react-i18next';
import { Badge } from '@/components/ui/badge';

export default function AdminTestsPage() {
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState('');
  const [sections, setSections] = useState([]);
  const [selectedSectionId, setSelectedSectionId] = useState('');

  const [loadingCourses, setLoadingCourses] = useState(false);
  const [loadingSections, setLoadingSections] = useState(false);
  const [saving, setSaving] = useState(false);

  const [questions, setQuestions] = useState([]);
  const { t } = useTranslation();
  const { adminUser } = useAuth();

  useEffect(() => {
    fetchCourses();
  }, []);

  useEffect(() => {
    if (selectedCourseId) {
      fetchSections(selectedCourseId);
    } else {
      setSections([]);
      setSelectedSectionId('');
      setQuestions([]);
    }
  }, [selectedCourseId]);

  useEffect(() => {
    if (selectedSectionId && sections.length > 0) {
      const section = sections.find(s => s.id.toString() === selectedSectionId);
      if (section && section.hasTest) {
        // Fetch test content
        adminGetSectionTestApi(section.id)
          .then(res => {
            try {
              const parsed = JSON.parse(res.data.data);
              setQuestions(Array.isArray(parsed) ? parsed : []);
            } catch (e) {
              setQuestions([]);
            }
          })
          .catch(err => {
            console.error("Failed to load test:", err);
            setQuestions([]);
          });
      } else {
        setQuestions([]);
      }
    } else {
      setQuestions([]);
    }
  }, [selectedSectionId, sections]);

  const fetchCourses = async () => {
    setLoadingCourses(true);
    try {
      const role = adminUser?.role;
      const res = role === 'INSTRUCTOR'
        ? await getInstructorCoursesApi({ size: 1000 })
        : await getAdminCoursesApi({ size: 1000 });
      const data = res.data.data;
      setCourses(data?.items ?? data?.content ?? (Array.isArray(data) ? data : []));
    } catch (err) {
      toast.error(t('ui.admin_test.load_courses_error'));
    } finally {
      setLoadingCourses(false);
    }
  };

  const fetchSections = async (courseId) => {
    setLoadingSections(true);
    try {
      const res = await adminGetSectionsApi(courseId);
      setSections(res.data.data || []);
      setSelectedSectionId('');
    } catch (err) {
      toast.error(t('ui.admin_test.load_sections_error'));
    } finally {
      setLoadingSections(false);
    }
  };

  const handleAddQuestion = () => {
    setQuestions([
      ...questions,
      {
        question: '',
        options: ['', '', '', ''],
        correctAnswer: ''
      }
    ]);
  };

  const handleRemoveQuestion = (index) => {
    const newQ = [...questions];
    newQ.splice(index, 1);
    setQuestions(newQ);
  };

  const handleQuestionChange = (index, field, value) => {
    const newQ = [...questions];
    newQ[index][field] = value;
    setQuestions(newQ);
  };

  const handleOptionChange = (qIndex, optIndex, value) => {
    const newQ = [...questions];
    newQ[qIndex].options[optIndex] = value;
    setQuestions(newQ);
  };

  const handleSave = async () => {
    if (!selectedSectionId) {
      toast.warning(t('ui.admin_test.warn_no_section'));
      return;
    }

    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      if (!q.question.trim()) {
        toast.error(t('ui.admin_test.empty_question', { num: i + 1 }));
        return;
      }
      if (q.options.some(o => !o.trim())) {
        toast.error(t('ui.admin_test.empty_options', { num: i + 1 }));
        return;
      }
      if (!q.correctAnswer) {
        toast.error(t('ui.admin_test.empty_correct', { num: i + 1 }));
        return;
      }
    }

    setSaving(true);
    try {
      await adminUpdateSectionTestApi(selectedSectionId, JSON.stringify(questions));
      toast.success(t('ui.admin_test.save_success'));
      fetchSections(selectedCourseId);
    } catch (err) {
      toast.error(err.response?.data?.message || t('ui.admin_test.save_error'));
    } finally {
      setSaving(false);
    }
  };

  const selectedSection = sections.find(s => s.id.toString() === selectedSectionId);
  const selectedCourse = courses.find(c => c.id.toString() === selectedCourseId);

  return (
    <div className="max-w-5xl mx-auto space-y-6 px-2">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <FileQuestion className="h-6 w-6 text-primary" />
            <h1 className="text-2xl font-bold tracking-tight">{t('ui.admin_test.title')}</h1>
          </div>
          <p className="text-muted-foreground text-sm">{t('ui.admin_test.subtitle')}</p>
        </div>
        {selectedSectionId && (
          <Button onClick={handleSave} disabled={saving} size="lg">
            {saving ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Save className="mr-2 h-4 w-4" />}
            {t('ui.admin_test.save_btn')}
          </Button>
        )}
      </div>

      {/* Selector card — full width, fields in row */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">{t('ui.admin_test.select_title')}</CardTitle>
          <CardDescription>{t('ui.admin_test.select_desc')}</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>{t('ui.admin_test.course_lbl')}</Label>
              <DropdownMenu>
                <DropdownMenuTrigger
                  render={<Button variant="outline" className="w-full justify-between font-normal bg-background" disabled={loadingCourses} />}
                >
                  {loadingCourses
                    ? <span className="text-muted-foreground flex items-center gap-1"><Loader2 className="h-3 w-3 animate-spin" /> Đang tải...</span>
                    : selectedCourseId
                      ? courses.find(c => c.id.toString() === selectedCourseId)?.title
                      : <span className="text-muted-foreground">{t('ui.admin_test.course_placeholder')}</span>
                  }
                  <ChevronDown className="h-4 w-4 opacity-50 shrink-0" />
                </DropdownMenuTrigger>
                <DropdownMenuContent className="w-(--anchor-width) min-w-[200px] max-h-[300px] overflow-y-auto" align="start">
                  {courses.map(c => (
                    <DropdownMenuItem key={c.id} onClick={() => setSelectedCourseId(c.id.toString())}>
                      {c.title}
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>
            </div>

            <div className="space-y-2">
              <Label>{t('ui.admin_test.section_lbl')}</Label>
              <DropdownMenu>
                <DropdownMenuTrigger
                  render={<Button variant="outline" className="w-full justify-between font-normal bg-background" disabled={!selectedCourseId || loadingSections} />}
                >
                  {loadingSections
                    ? <span className="text-muted-foreground flex items-center gap-1"><Loader2 className="h-3 w-3 animate-spin" /> Đang tải...</span>
                    : selectedSectionId
                      ? sections.find(s => s.id.toString() === selectedSectionId)?.title
                      : <span className="text-muted-foreground">{(!selectedCourseId ? 'Chọn khóa học trước...' : t('ui.admin_test.section_placeholder'))}</span>
                  }
                  <ChevronDown className="h-4 w-4 opacity-50 shrink-0" />
                </DropdownMenuTrigger>
                <DropdownMenuContent className="w-(--anchor-width) min-w-[200px] max-h-[300px] overflow-y-auto" align="start">
                  {sections.map(s => (
                    <DropdownMenuItem key={s.id} onClick={() => setSelectedSectionId(s.id.toString())} className="flex justify-between items-center">
                      {s.title}
                      {s.testContent && (
                        <Badge variant="secondary" className="ml-2 text-[10px] py-0">✓ Có test</Badge>
                      )}
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>

          {/* breadcrumb info */}
          {selectedSection && (
            <div className="mt-3 flex items-center gap-2 text-sm text-muted-foreground">
              <span>{selectedCourse?.title}</span>
              <ChevronDown className="h-3 w-3 rotate-[-90deg]" />
              <span className="font-medium text-foreground">{selectedSection.title}</span>
              {selectedSection.hasTest
                ? <Badge variant="outline" className="text-green-600 border-green-300 text-xs">Đã có bài test ({questions.length} câu)</Badge>
                : <Badge variant="outline" className="text-orange-500 border-orange-300 text-xs">Chưa có bài test</Badge>
              }
            </div>
          )}
        </CardContent>
      </Card>

      {/* Questions area */}
      {selectedSectionId && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">{t('ui.admin_test.compose_title')}</h2>
            <span className="text-sm text-muted-foreground">{questions.length} câu hỏi</span>
          </div>

          {questions.length === 0 && (
            <Card className="border-dashed">
              <CardContent className="flex flex-col items-center justify-center py-12 text-center">
                <FileQuestion className="h-10 w-10 text-muted-foreground/40 mb-3" />
                <p className="text-muted-foreground text-sm">Chưa có câu hỏi nào. Nhấn nút bên dưới để thêm.</p>
              </CardContent>
            </Card>
          )}

          {questions.map((q, qIdx) => (
            <Card key={qIdx} className="relative">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-sm font-semibold text-muted-foreground">
                    {t('ui.admin_test.question_lbl', { num: qIdx + 1 })}
                  </CardTitle>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7 text-destructive hover:bg-destructive/10"
                    onClick={() => handleRemoveQuestion(qIdx)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <Textarea
                  value={q.question}
                  onChange={(e) => handleQuestionChange(qIdx, 'question', e.target.value)}
                  placeholder={t('ui.admin_test.question_placeholder')}
                  className="resize-none"
                  rows={2}
                />

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {q.options.map((opt, optIdx) => (
                    <div key={optIdx} className="flex items-center gap-2">
                      <span className={`flex-shrink-0 flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold border ${q.correctAnswer === opt && opt
                        ? 'bg-green-500 text-white border-green-500'
                        : 'border-input text-muted-foreground'
                        }`}>
                        {String.fromCharCode(65 + optIdx)}
                      </span>
                      <Input
                        value={opt}
                        onChange={(e) => handleOptionChange(qIdx, optIdx, e.target.value)}
                        placeholder={t('ui.admin_test.option_placeholder', { char: String.fromCharCode(65 + optIdx) })}
                        className="flex-1"
                      />
                    </div>
                  ))}
                </div>

                <div className="flex items-center gap-3 pt-1">
                  <Label className="text-sm shrink-0">{t('ui.admin_test.correct_lbl')}:</Label>
                  <DropdownMenu>
                    <DropdownMenuTrigger
                      render={<Button variant="outline" className="w-[280px] justify-between font-normal bg-background" />}
                    >
                      {q.correctAnswer
                        ? <span className="truncate"><span className="font-medium">{String.fromCharCode(65 + q.options.findIndex(o => o === q.correctAnswer))}.</span> {q.correctAnswer}</span>
                        : <span className="text-muted-foreground">{t('ui.admin_test.correct_placeholder')}</span>
                      }
                      <ChevronDown className="h-4 w-4 opacity-50 shrink-0" />
                    </DropdownMenuTrigger>
                    <DropdownMenuContent className="w-[280px]" align="start">
                      {q.options.map((opt, optIdx) => opt ? (
                        <DropdownMenuItem key={optIdx} onClick={() => handleQuestionChange(qIdx, 'correctAnswer', opt)}>
                          <span className="font-medium mr-2">{String.fromCharCode(65 + optIdx)}.</span> {opt}
                        </DropdownMenuItem>
                      ) : null)}
                    </DropdownMenuContent>
                  </DropdownMenu>
                  {q.correctAnswer && (
                    <Badge variant="outline" className="text-green-600 border-green-300 text-xs">✓ Đã chọn</Badge>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}

          <Button
            variant="outline"
            className="w-full border-dashed h-12 text-muted-foreground hover:text-foreground hover:border-primary"
            onClick={handleAddQuestion}
          >
            <Plus className="mr-2 h-4 w-4" />
            {t('ui.admin_test.add_btn')}
          </Button>

          {questions.length > 0 && (
            <div className="flex justify-end pb-6">
              <Button onClick={handleSave} disabled={saving} size="lg">
                {saving ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Save className="mr-2 h-4 w-4" />}
                {t('ui.admin_test.save_btn')}
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
