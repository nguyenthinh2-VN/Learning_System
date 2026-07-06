import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { getSectionTestApi, submitSectionTestApi } from '@/api/course';
import { Button } from '@/components/ui/button';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Label } from '@/components/ui/label';
import { Loader2, CheckCircle2, XCircle, Trophy } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function SectionTestView({ sectionId, onTestPassed }) {
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [answers, setAnswers] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const { t } = useTranslation();

  useEffect(() => {
    fetchTest();
  }, [sectionId]);

  const fetchTest = async () => {
    setLoading(true);
    setResult(null);
    setAnswers({});
    try {
      const res = await getSectionTestApi(sectionId);
      setQuestions(res.data.data || []);
    } catch (err) {
      toast.error(t(err.response?.data?.code ? `api.${err.response.data.code}` : 'test.load_error'));
    } finally {
      setLoading(false);
    }
  };

  const handleAnswerChange = (qIndex, value) => {
    setAnswers(prev => ({
      ...prev,
      [qIndex]: value
    }));
  };

  const handleSubmit = async () => {
    if (Object.keys(answers).length < questions.length) {
      toast.warning(t('test.warn_incomplete'));
      return;
    }
    setSubmitting(true);
    try {
      const res = await submitSectionTestApi(sectionId, answers);
      const data = res.data.data;
      setResult(data);
      if (data.passed) {
        toast.success(t('test.success_passed', { score: data.score }));
        onTestPassed && onTestPassed();
      } else {
        toast.error(t('test.failed', { score: data.score }));
      }
    } catch (err) {
      toast.error(t(err.response?.data?.code ? `api.${err.response.data.code}` : 'test.submit_error'));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64 bg-slate-50 rounded-xl border">
        <Loader2 className="animate-spin text-indigo-500" />
      </div>
    );
  }

  if (!questions || questions.length === 0) {
    return (
      <div className="flex justify-center items-center h-64 bg-slate-50 rounded-xl border">
        <p className="text-muted-foreground">{t('test.no_test')}</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl border p-6 max-h-[70vh] overflow-y-auto shadow-sm">
      <div className="flex items-center gap-3 mb-6 pb-4 border-b">
        <div className="w-10 h-10 rounded-full bg-indigo-100 flex items-center justify-center shrink-0">
          <Trophy className="w-5 h-5 text-indigo-600" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-slate-900">{t('test.title')}</h2>
          <p className="text-sm text-slate-500">{t('test.subtitle')}</p>
        </div>
      </div>

      {result ? (
        <div className={`p-6 rounded-xl border text-center space-y-4 mb-6 ${result.passed ? 'bg-emerald-50 border-emerald-200' : 'bg-red-50 border-red-200'}`}>
          {result.passed ? (
            <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto" />
          ) : (
            <XCircle className="w-12 h-12 text-red-500 mx-auto" />
          )}
          <div>
            <h3 className={`text-xl font-bold ${result.passed ? 'text-emerald-700' : 'text-red-700'}`}>
              {result.passed ? t('test.passed_title') : t('test.failed_title')}
            </h3>
            <p className="text-slate-600 mt-2">
              {t('test.result_desc', { correct: result.correctCount, total: result.totalQuestions, score: result.score })}
            </p>
          </div>
          <Button onClick={fetchTest} variant="outline" className="mt-4">
            {t('test.retry')}
          </Button>
        </div>
      ) : (
        <div className="space-y-8">
          {questions.map((q, idx) => (
            <div key={idx} className="space-y-3">
              <h4 className="font-semibold text-slate-800">
                <span className="text-indigo-600 mr-2">{t('test.question_prefix', { num: idx + 1 })}</span>
                {q.question}
              </h4>
              <RadioGroup value={answers[idx] || ''} onValueChange={(val) => handleAnswerChange(idx, val)} className="space-y-2">
                {q.options?.map((opt, oIdx) => opt ? (
                  <div key={oIdx} className="flex items-center space-x-2 border p-3 rounded-lg hover:bg-slate-50 transition-colors cursor-pointer" onClick={() => handleAnswerChange(idx, opt)}>
                    <RadioGroupItem value={opt} id={`q-${idx}-opt-${oIdx}`} />
                    <Label htmlFor={`q-${idx}-opt-${oIdx}`} className="flex-1 cursor-pointer">{opt}</Label>
                  </div>
                ) : null)}
              </RadioGroup>
            </div>
          ))}

          <div className="pt-4 border-t flex justify-end">
            <Button onClick={handleSubmit} disabled={submitting} className="min-w-[150px]">
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {t('test.submit_btn')}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
