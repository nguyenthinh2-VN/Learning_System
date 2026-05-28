import { Button } from '@/components/ui/button';
import { BookOpen } from 'lucide-react';

export default function HeroSection() {
  const scrollToCourses = () => {
    const el = document.getElementById('course-list-section');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <section className="px-6 py-16 md:py-24">
      <div className="max-w-5xl mx-auto">
        <div className="grid md:grid-cols-2 gap-12 items-center">
          {/* Text */}
          <div>
            <h1 className="text-3xl md:text-4xl font-bold tracking-tight leading-tight">
              Học mọi lúc,
              <br />
              mọi nơi
            </h1>
            <p className="mt-4 text-muted-foreground leading-relaxed max-w-md">
              Khám phá hàng trăm khóa học chất lượng cao từ các giảng viên hàng đầu.
              Nâng cao kỹ năng và phát triển sự nghiệp ngay hôm nay.
            </p>
            <Button
              id="hero-cta"
              size="lg"
              className="mt-6 gap-2"
              onClick={scrollToCourses}
            >
              <BookOpen className="size-4" />
              Khám phá khóa học
            </Button>
          </div>

          {/* Illustration placeholder */}
          <div className="hidden md:flex items-center justify-center">
            <div className="w-full max-w-xs aspect-square rounded-2xl bg-muted flex items-center justify-center">
              <div className="text-center text-muted-foreground">
                <BookOpen className="size-16 mx-auto mb-3 opacity-20" />
                <p className="text-xs opacity-40">Illustration</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
