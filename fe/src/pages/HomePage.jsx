import HeroSection from '@/features/home/HeroSection';
import ProgressSection from '@/features/home/ProgressSection';
import StatsBar from '@/features/home/StatsBar';
import Testimonials from '@/features/home/Testimonials';
import FeaturedCourses from '@/features/home/FeaturedCourses';
import CourseListSection from '@/features/home/CourseListSection';
import Footer from '@/components/layout/Footer';

export default function HomePage() {
  return (
    <div className="flex flex-col min-h-[calc(100vh-3rem)]">
      <HeroSection />
      <ProgressSection />
      <StatsBar />
      <Testimonials />
      <FeaturedCourses />
      <CourseListSection />
      <Footer />
    </div>
  );
}
