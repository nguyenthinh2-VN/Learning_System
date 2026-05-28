export default function Footer() {
  return (
    <footer className="border-t bg-foreground text-background/60 mt-auto">
      <div className="max-w-6xl mx-auto px-6 py-8">
        <div className="flex flex-col md:flex-row justify-between items-center gap-4">
          {/* Brand */}
          <div className="text-center md:text-left">
            <p className="text-sm font-semibold text-background tracking-tight">
              LearnSpace
            </p>
            <p className="text-xs text-background/40 mt-1">
              Nền tảng học trực tuyến hàng đầu
            </p>
          </div>

          {/* Links */}
          <div className="flex gap-6 text-xs text-background/40">
            <span className="hover:text-background/70 cursor-pointer transition-colors">
              Điều khoản
            </span>
            <span className="hover:text-background/70 cursor-pointer transition-colors">
              Chính sách
            </span>
            <span className="hover:text-background/70 cursor-pointer transition-colors">
              Liên hệ
            </span>
          </div>
        </div>

        {/* Copyright */}
        <div className="mt-6 pt-4 border-t border-background/10 text-center">
          <p className="text-xs text-background/30">
            © {new Date().getFullYear()} LearnSpace. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}
