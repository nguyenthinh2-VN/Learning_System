import '@/styles/brand.css';
import { Button } from '@/components/ui/button';
import { BookOpen, Star, Users, GraduationCap, PlayCircle, TrendingUp } from 'lucide-react';

// Dữ liệu giả định cho floating card
const FEATURED_COURSE = {
  title: 'Spring Boot Clean Architecture',
  students: 512,
  rating: 4.9,
  tag: 'Bán chạy nhất',
};

const STATS = [
  { icon: BookOpen, value: '500+', label: 'Khóa học' },
  { icon: Users,    value: '10k+', label: 'Học viên' },
  { icon: Star,     value: '4.8',  label: 'Đánh giá' },
];

// Avatar stack giả định
const AVATARS = ['N', 'T', 'A', 'M'];

export default function HeroSection() {
  const scrollToCourses = () => {
    const el = document.getElementById('course-list-section');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <section
      className="px-6 py-16 md:py-24 overflow-hidden"
      style={{ background: 'var(--brand-gradient-hero)' }}
    >
      <div className="max-w-5xl mx-auto">
        <div className="grid md:grid-cols-2 gap-12 items-center">

          {/* ── LEFT: Text content ── */}
          <div className="space-y-6">
            {/* Badge */}
            <span className="brand-badge">
              <Star className="size-3 fill-current" style={{ color: 'var(--brand-primary)' }} />
              Nền tảng học #1 Việt Nam
            </span>

            {/* Heading */}
            <div>
              <h1
                className="text-4xl md:text-5xl font-extrabold leading-tight tracking-tight"
                style={{ color: 'var(--brand-text-primary)' }}
              >
                Học mọi lúc,
                <br />
                <span style={{
                  background: 'var(--brand-gradient-btn)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  backgroundClip: 'text',
                }}>
                  mọi nơi
                </span>
              </h1>
              <p
                className="mt-4 text-base leading-relaxed max-w-md"
                style={{ color: 'var(--brand-text-secondary)' }}
              >
                Khám phá hàng trăm khóa học chất lượng cao từ các giảng viên hàng đầu.
                Nâng cao kỹ năng và phát triển sự nghiệp ngay hôm nay.
              </p>
            </div>

            {/* CTA Buttons */}
            <div className="flex items-center gap-3 flex-wrap">
              <button
                id="hero-cta"
                className="brand-btn-primary flex items-center gap-2 px-6 py-3 rounded-xl text-sm font-semibold shadow-md"
                onClick={scrollToCourses}
              >
                <BookOpen className="size-4" />
                Khám phá khóa học
              </button>
              <button
                className="flex items-center gap-2 px-6 py-3 rounded-xl text-sm font-semibold border transition-colors"
                style={{
                  borderColor: 'var(--brand-primary-mid)',
                  color: 'var(--brand-primary)',
                  background: 'var(--brand-primary-light)',
                }}
                onClick={scrollToCourses}
              >
                <PlayCircle className="size-4" />
                Xem demo miễn phí
              </button>
            </div>

            {/* Stats */}
            <div
              className="flex items-center gap-6 pt-2 border-t"
              style={{ borderColor: 'var(--brand-border)' }}
            >
              {STATS.map(({ icon: Icon, value, label }, i) => (
                <div key={i} className="flex items-center gap-2">
                  <div
                    className="size-8 rounded-lg flex items-center justify-center"
                    style={{ background: 'var(--brand-primary-light)' }}
                  >
                    <Icon className="size-4" style={{ color: 'var(--brand-primary)' }} />
                  </div>
                  <div>
                    <p className="brand-stat-number">{value}</p>
                    <p className="brand-stat-label">{label}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* ── RIGHT: Illustration card ── */}
          <div className="hidden md:flex items-center justify-center">
            <div className="relative w-full max-w-sm">

              {/* Main card */}
              <div
                className="rounded-3xl p-6 space-y-5 shadow-xl"
                style={{
                  background: 'linear-gradient(145deg, #EFF6FF 0%, #F5F3FF 100%)',
                  border: '1px solid var(--brand-primary-mid)',
                  boxShadow: 'var(--brand-shadow-lg)',
                }}
              >
                {/* Brand header */}
                <div className="flex items-center gap-3">
                  <div
                    className="size-12 rounded-2xl flex items-center justify-center shadow-md"
                    style={{ background: 'var(--brand-gradient-btn)' }}
                  >
                    <GraduationCap className="size-6 text-white" />
                  </div>
                  <div>
                    <p className="font-bold text-base" style={{ color: 'var(--brand-text-primary)' }}>
                      LearnSpace
                    </p>
                    <p className="text-xs" style={{ color: 'var(--brand-text-secondary)' }}>
                      Học không giới hạn
                    </p>
                  </div>
                </div>

                {/* Featured course card */}
                <div
                  className="rounded-2xl p-4 space-y-3"
                  style={{
                    background: '#FFFFFF',
                    border: '1px solid var(--brand-border)',
                    boxShadow: 'var(--brand-shadow-sm)',
                  }}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <span
                        className="text-[10px] font-semibold px-2 py-0.5 rounded-full"
                        style={{
                          background: 'var(--brand-primary-light)',
                          color: 'var(--brand-primary)',
                        }}
                      >
                        {FEATURED_COURSE.tag}
                      </span>
                      <p
                        className="text-sm font-semibold mt-1.5 line-clamp-2 leading-snug"
                        style={{ color: 'var(--brand-text-primary)' }}
                      >
                        {FEATURED_COURSE.title}
                      </p>
                    </div>
                    <div
                      className="size-10 rounded-xl flex items-center justify-center shrink-0"
                      style={{ background: 'var(--brand-primary-light)' }}
                    >
                      <BookOpen className="size-5" style={{ color: 'var(--brand-primary)' }} />
                    </div>
                  </div>

                  {/* Rating */}
                  <div className="flex items-center gap-1.5">
                    {[1,2,3,4,5].map((s) => (
                      <Star
                        key={s}
                        className="size-3 fill-current"
                        style={{ color: '#F59E0B' }}
                      />
                    ))}
                    <span className="text-xs font-semibold ml-1" style={{ color: 'var(--brand-text-primary)' }}>
                      {FEATURED_COURSE.rating}
                    </span>
                  </div>

                  {/* Progress */}
                  <div>
                    <div className="flex justify-between text-[11px] mb-1" style={{ color: 'var(--brand-text-secondary)' }}>
                      <span>Tiến độ học</span>
                      <span style={{ color: 'var(--brand-success)' }}>78%</span>
                    </div>
                    <div className="h-1.5 rounded-full" style={{ background: 'var(--brand-primary-mid)' }}>
                      <div
                        className="h-full rounded-full transition-all"
                        style={{ width: '78%', background: 'var(--brand-gradient-btn)' }}
                      />
                    </div>
                  </div>
                </div>

                {/* Avatar stack + student count */}
                <div className="flex items-center gap-3">
                  <div className="flex -space-x-2">
                    {AVATARS.map((letter, i) => (
                      <div
                        key={i}
                        className="size-8 rounded-full border-2 border-white flex items-center justify-center text-xs font-bold text-white"
                        style={{
                          background: `var(--brand-gradient-btn)`,
                          opacity: 1 - i * 0.1,
                        }}
                      >
                        {letter}
                      </div>
                    ))}
                  </div>
                  <div>
                    <p className="text-xs font-semibold" style={{ color: 'var(--brand-text-primary)' }}>
                      +{FEATURED_COURSE.students.toLocaleString()} học viên
                    </p>
                    <p className="text-[11px]" style={{ color: 'var(--brand-text-secondary)' }}>
                      đang học ngay hôm nay
                    </p>
                  </div>
                </div>

                {/* Trending badge */}
                <div
                  className="flex items-center gap-2 px-3 py-2 rounded-xl"
                  style={{ background: 'var(--brand-success-light)', border: '1px solid #A7F3D0' }}
                >
                  <TrendingUp className="size-4" style={{ color: 'var(--brand-success)' }} />
                  <p className="text-xs font-medium" style={{ color: 'var(--brand-success)' }}>
                    Tăng trưởng 32% học viên mới tháng này
                  </p>
                </div>
              </div>

              {/* Decorative blobs */}
              <div
                className="absolute -top-4 -right-4 size-16 rounded-full blur-2xl opacity-40 pointer-events-none"
                style={{ background: 'var(--brand-primary)' }}
              />
              <div
                className="absolute -bottom-4 -left-4 size-12 rounded-full blur-xl opacity-30 pointer-events-none"
                style={{ background: 'var(--brand-secondary)' }}
              />
            </div>
          </div>

        </div>
      </div>
    </section>
  );
}
