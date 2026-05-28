import '@/styles/brand.css';
import { useNavigate } from 'react-router-dom';
import { Users, BookOpen } from 'lucide-react';

function formatPrice(price) {
  if (price === 0) return 'Miễn phí';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(price);
}

export default function CourseCard({ id, title, description, price, enrolledCount, maxStudents, thumbnailUrl }) {
  const navigate = useNavigate();
  const progress = maxStudents > 0 ? Math.round((enrolledCount / maxStudents) * 100) : 0;
  const isFull = enrolledCount >= maxStudents;

  return (
    <div
      id={`course-card-${id}`}
      className="group bg-white rounded-xl overflow-hidden cursor-pointer transition-all duration-200"
      style={{
        border: '1px solid var(--brand-border)',
        boxShadow: 'var(--brand-shadow-sm)',
      }}
      onClick={() => navigate(`/courses/${id}`)}
      onMouseEnter={(e) => { e.currentTarget.style.boxShadow = 'var(--brand-shadow-md)'; e.currentTarget.style.transform = 'translateY(-2px)'; }}
      onMouseLeave={(e) => { e.currentTarget.style.boxShadow = 'var(--brand-shadow-sm)'; e.currentTarget.style.transform = 'translateY(0)'; }}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && navigate(`/courses/${id}`)}
    >
      {/* Thumbnail */}
      <div className="aspect-video relative overflow-hidden" style={{ background: 'var(--brand-primary-light)' }}>
        {thumbnailUrl ? (
          <img
            src={thumbnailUrl}
            alt={title}
            className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
            onError={(e) => { e.target.style.display = 'none'; }}
          />
        ) : (
          <div className="absolute inset-0 flex items-center justify-center">
            <BookOpen className="size-10" style={{ color: 'var(--brand-primary-mid)' }} />
          </div>
        )}
        {isFull && (
          <span
            className="absolute top-2 right-2 text-[10px] font-semibold px-2 py-0.5 rounded-full text-white"
            style={{ background: '#EF4444' }}
          >
            Hết chỗ
          </span>
        )}
      </div>

      {/* Content */}
      <div className="p-4 space-y-3">
        <h3
          className="font-semibold text-sm leading-snug line-clamp-2"
          style={{ color: 'var(--brand-text-primary)' }}
        >
          {title}
        </h3>

        {description && (
          <p className="text-xs line-clamp-2 leading-relaxed" style={{ color: 'var(--brand-text-secondary)' }}>
            {description}
          </p>
        )}

        {/* Price */}
        <p className="text-sm font-bold" style={{ color: price === 0 ? 'var(--brand-success)' : 'var(--brand-success)' }}>
          {formatPrice(price)}
        </p>

        {/* Progress */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-[11px]" style={{ color: 'var(--brand-text-secondary)' }}>
            <span className="flex items-center gap-1">
              <Users className="size-3" />
              {enrolledCount}/{maxStudents} học viên
            </span>
            <span>{progress}%</span>
          </div>
          <div className="h-1.5 rounded-full" style={{ background: 'var(--brand-primary-mid)' }}>
            <div
              className="h-full rounded-full transition-all duration-300"
              style={{ width: `${Math.min(progress, 100)}%`, background: 'var(--brand-primary)' }}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
