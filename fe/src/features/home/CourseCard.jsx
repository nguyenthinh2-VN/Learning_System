import { useNavigate } from 'react-router-dom';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Users, BookOpen } from 'lucide-react';

function formatPrice(price) {
  if (price === 0) return 'Miễn phí';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(price);
}

export default function CourseCard({ id, title, description, price, enrolledCount, maxStudents }) {
  const navigate = useNavigate();
  const progress = maxStudents > 0 ? Math.round((enrolledCount / maxStudents) * 100) : 0;
  const isFull = enrolledCount >= maxStudents;

  return (
    <Card
      id={`course-card-${id}`}
      className="group overflow-hidden transition-shadow hover:shadow-md cursor-pointer"
      onClick={() => navigate(`/courses/${id}`)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && navigate(`/courses/${id}`)}
    >
      {/* Thumbnail placeholder */}
      <div className="aspect-video bg-muted flex items-center justify-center relative">
        <BookOpen className="size-10 text-muted-foreground/20" />
        {isFull && (
          <Badge className="absolute top-2 right-2 bg-foreground text-background text-[10px]">
            Hết chỗ
          </Badge>
        )}
      </div>

      <CardContent className="p-4 space-y-3">
        {/* Title */}
        <h3 className="font-semibold text-sm leading-snug line-clamp-2 group-hover:underline underline-offset-2">
          {title}
        </h3>

        {/* Description */}
        {description && (
          <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
            {description}
          </p>
        )}

        {/* Price */}
        <p className="text-sm font-bold">{formatPrice(price)}</p>

        {/* Enrollment progress */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-[11px] text-muted-foreground">
            <span className="flex items-center gap-1">
              <Users className="size-3" />
              {enrolledCount}/{maxStudents} học viên
            </span>
            <span>{progress}%</span>
          </div>
          <div className="h-1.5 bg-muted rounded-full overflow-hidden">
            <div
              className="h-full bg-foreground/70 rounded-full transition-all duration-300"
              style={{ width: `${Math.min(progress, 100)}%` }}
            />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
