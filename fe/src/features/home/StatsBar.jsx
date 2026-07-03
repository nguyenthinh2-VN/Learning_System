import { useTranslation } from 'react-i18next';
import '@/styles/brand.css';
import { Users, BookOpen, GraduationCap, Star } from 'lucide-react';

export default function StatsBar() {
  const { t } = useTranslation();

  const stats = [
    { icon: BookOpen, value: '500+', label: t('ui.home.quality_courses') },
    { icon: Users, value: '10k+', label: t('ui.home.trusted_students') },
    { icon: GraduationCap, value: '200+', label: t('ui.home.top_instructors') },
    { icon: Star, value: '4.8', label: t('ui.home.avg_rating') },
  ];

  return (
    <section className="px-6 py-10" style={{ background: 'var(--brand-bg-section)' }}>
      <div className="max-w-5xl mx-auto">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {stats.map((stat) => (
            <div
              key={stat.label}
              className="flex items-center gap-3 bg-white rounded-xl border px-5 py-4"
              style={{ borderColor: 'var(--brand-border)' }}
            >
              <div
                className="size-9 rounded-lg flex items-center justify-center shrink-0"
                style={{ background: 'var(--brand-primary-light)' }}
              >
                <stat.icon className="size-4" style={{ color: 'var(--brand-primary)' }} />
              </div>
              <div>
                <p className="text-xl font-bold leading-tight" style={{ color: 'var(--brand-primary)' }}>
                  {stat.value}
                </p>
                <p className="text-xs mt-0.5" style={{ color: 'var(--brand-text-secondary)' }}>
                  {stat.label}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
