import { useTranslation } from 'react-i18next';
import { useAuth } from '@/context/AuthContext';
import { Flame, Play, TrendingUp, ImageIcon } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function ProgressSection() {
  const { t } = useTranslation();
  const { isPublicAuthenticated } = useAuth();

  if (!isPublicAuthenticated) return null;

  return (
    <section className="py-8 bg-slate-50/50">
      <div className="container mx-auto px-4 max-w-7xl">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold flex items-center gap-2 text-slate-900">
            <TrendingUp className="w-6 h-6 text-indigo-500" />
            Tiến độ của bạn
          </h2>
          <Link to="/progress" className="text-sm font-semibold text-indigo-600 hover:text-indigo-700">
            Xem tất cả
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Card 1: Streak */}
          <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-100 flex items-center gap-4">
            <div className="w-12 h-12 rounded-full bg-indigo-100 flex items-center justify-center shrink-0">
              <Flame className="w-6 h-6 text-indigo-500" />
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium">{t('ui.home.learning_streak')}</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-0.5">14 {t('ui.home.days')}</h3>
            </div>
          </div>

          {/* Card 2: Course Progress */}
          <div className="md:col-span-2 bg-white rounded-2xl p-5 shadow-sm border border-slate-100 flex flex-col sm:flex-row items-center gap-5">
            <div className="w-16 h-16 rounded-2xl bg-slate-100 flex items-center justify-center shrink-0">
              <ImageIcon className="w-6 h-6 text-slate-400" />
            </div>
            <div className="flex-1 w-full">
              <h3 className="text-base font-bold text-slate-900">React & Spring Boot Fullstack</h3>
              <p className="text-xs text-slate-500 mt-1">{t('ui.home.chapter')} 4: Authentication & Authorization</p>

              <div className="mt-3 flex items-center gap-3">
                <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                  <div className="h-full bg-indigo-500 rounded-full" style={{ width: '45%' }}></div>
                </div>
                <span className="text-xs font-bold text-indigo-600">45%</span>
              </div>
            </div>
            <button className="w-10 h-10 rounded-full bg-indigo-500 hover:bg-indigo-600 flex items-center justify-center shrink-0 transition-colors shadow-sm mt-4 sm:mt-0">
              <Play className="w-4 h-4 text-white ml-0.5" />
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
