import { Users, BookOpen, GraduationCap } from 'lucide-react';

const stats = [
  { icon: Users, value: '1,000+', label: 'Học viên' },
  { icon: BookOpen, value: '50+', label: 'Khóa học' },
  { icon: GraduationCap, value: '30+', label: 'Giảng viên' },
];

export default function StatsBar() {
  return (
    <section className="px-6 py-10 bg-muted/50">
      <div className="max-w-5xl mx-auto">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
          {stats.map((stat) => (
            <div
              key={stat.label}
              className="flex items-center gap-4 bg-background rounded-lg border px-6 py-5"
            >
              <div className="flex items-center justify-center size-10 rounded-lg bg-foreground/5">
                <stat.icon className="size-5 text-foreground/60" />
              </div>
              <div>
                <p className="text-2xl font-bold tracking-tight">{stat.value}</p>
                <p className="text-xs text-muted-foreground">{stat.label}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
