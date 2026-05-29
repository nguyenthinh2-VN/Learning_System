import '@/styles/brand.css';
import { Star, Quote, BadgeCheck, TrendingUp } from 'lucide-react';

// Featured testimonial — câu chuyện nổi bật
const FEATURED = {
  name: 'Nguyễn Minh Anh',
  role: 'Frontend Developer @ FPT Software',
  quote:
    'Sau 3 tháng theo lộ trình trên LearnSpace, mình đã chuyển từ tester sang frontend developer. Khóa học thực chiến, mentor phản hồi cực nhanh và cộng đồng học viên rất nhiệt.',
  rating: 5,
  course: 'React & Spring Boot Fullstack',
  from: '#6366F1',
  to: '#8B5CF6',
};

// Các review phụ
const REVIEWS = [
  {
    name: 'Trần Quốc Bảo',
    role: 'Sinh viên năm 3 — Bách Khoa',
    quote: 'Giải thích dễ hiểu, bài tập sát thực tế. Mình ôn thi và làm đồ án tốt nghiệp đều dựa vào đây.',
    rating: 5,
    from: '#0EA5E9', to: '#2563EB',
  },
  {
    name: 'Phạm Thu Hà',
    role: 'Digital Marketer',
    quote: 'Học buổi tối sau giờ làm vẫn theo kịp vì nội dung chia nhỏ rõ ràng. Đáng đồng tiền.',
    rating: 5,
    from: '#EC4899', to: '#F43F5E',
  },
  {
    name: 'Lê Hoàng Nam',
    role: 'Freelance Designer',
    quote: 'Chứng chỉ hoàn thành giúp portfolio của mình thuyết phục khách hàng hơn hẳn.',
    rating: 4,
    from: '#10B981', to: '#0D9488',
  },
];

function Stars({ count }) {
  return (
    <div className="flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((s) => (
        <Star
          key={s}
          className="size-3.5"
          style={{
            color: s <= count ? '#F59E0B' : 'var(--brand-border)',
            fill: s <= count ? '#F59E0B' : 'transparent',
          }}
        />
      ))}
    </div>
  );
}

function Avatar({ name, from, to, size = 'size-11' }) {
  const initials = name.split(' ').slice(-2).map((w) => w[0]).join('').toUpperCase();
  return (
    <div
      className={`${size} rounded-full flex items-center justify-center text-white font-bold shrink-0 shadow-md`}
      style={{ background: `linear-gradient(135deg, ${from}, ${to})` }}
    >
      {initials}
    </div>
  );
}

export default function Testimonials() {
  return (
    <section className="relative px-6 py-16 overflow-hidden" style={{ background: 'var(--brand-bg-section)' }}>
      {/* Decorative blobs */}
      <div
        className="absolute top-0 -left-16 size-64 rounded-full blur-3xl opacity-20 pointer-events-none"
        style={{ background: 'var(--brand-primary)' }}
      />
      <div
        className="absolute bottom-0 -right-16 size-64 rounded-full blur-3xl opacity-20 pointer-events-none"
        style={{ background: 'var(--brand-secondary)' }}
      />

      <div className="relative max-w-5xl mx-auto">
        {/* Heading */}
        <div className="text-center mb-12">
          <span className="brand-badge mx-auto">
            <BadgeCheck className="size-3.5" style={{ color: 'var(--brand-primary)' }} />
            Hơn 10.000 học viên tin tưởng
          </span>
          <h2 className="text-2xl md:text-3xl font-bold mt-3" style={{ color: 'var(--brand-text-primary)' }}>
            Học viên nói gì về chúng tôi
          </h2>
          <p className="text-sm mt-1.5" style={{ color: 'var(--brand-text-secondary)' }}>
            Những câu chuyện thật từ cộng đồng LearnSpace
          </p>
        </div>

        <div className="grid lg:grid-cols-5 gap-6 items-stretch">
          {/* ── Featured testimonial (lớn) ── */}
          <div
            className="lg:col-span-2 relative rounded-3xl p-7 flex flex-col justify-between text-white overflow-hidden"
            style={{
              background: `linear-gradient(145deg, ${FEATURED.from}, ${FEATURED.to})`,
              boxShadow: '0 12px 40px rgba(99, 102, 241, 0.3)',
            }}
          >
            <Quote className="absolute top-5 right-5 size-20 opacity-15" />
            <div className="relative">
              <Stars count={FEATURED.rating} />
              <p className="text-base leading-relaxed mt-4 font-medium">
                “{FEATURED.quote}”
              </p>
            </div>
            <div className="relative mt-6">
              <div
                className="inline-flex items-center gap-1.5 text-[11px] font-semibold px-2.5 py-1 rounded-full mb-4"
                style={{ background: 'rgba(255,255,255,0.18)' }}
              >
                <TrendingUp className="size-3" />
                {FEATURED.course}
              </div>
              <div className="flex items-center gap-3">
                <Avatar name={FEATURED.name} from="#ffffff" to="#e0e7ff" size="size-12" />
                <div>
                  <p className="font-bold text-sm">{FEATURED.name}</p>
                  <p className="text-xs opacity-80">{FEATURED.role}</p>
                </div>
              </div>
            </div>
          </div>

          {/* ── Review nhỏ (stack) ── */}
          <div className="lg:col-span-3 grid sm:grid-cols-1 gap-4">
            {REVIEWS.map((r) => (
              <div
                key={r.name}
                className="group rounded-2xl p-5 flex gap-4 transition-all duration-200 hover:-translate-y-0.5"
                style={{
                  background: 'var(--brand-bg-card)',
                  border: '1px solid var(--brand-border)',
                  boxShadow: 'var(--brand-shadow-sm)',
                }}
              >
                <Avatar name={r.name} from={r.from} to={r.to} />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-2">
                    <div className="min-w-0">
                      <p className="font-semibold text-sm truncate" style={{ color: 'var(--brand-text-primary)' }}>
                        {r.name}
                      </p>
                      <p className="text-xs truncate" style={{ color: 'var(--brand-text-secondary)' }}>
                        {r.role}
                      </p>
                    </div>
                    <Stars count={r.rating} />
                  </div>
                  <p className="text-sm mt-2 leading-relaxed" style={{ color: 'var(--brand-text-secondary)' }}>
                    {r.quote}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
