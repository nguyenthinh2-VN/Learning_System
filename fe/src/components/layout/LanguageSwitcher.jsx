import { useTranslation } from 'react-i18next';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Globe } from 'lucide-react';
import { SidebarMenuButton } from '@/components/ui/sidebar';

export default function LanguageSwitcher() {
  const { i18n } = useTranslation();

  const changeLanguage = (value) => {
    // Lưu ý: languagedetector tự động xử lý localStorage
    i18n.changeLanguage(value);
  };

  // Xác định ngôn ngữ hiện tại (trường hợp fallback hoặc detector trả về chữ thường)
  const rawLang = i18n.language || 'vi';
  const currentLang = rawLang.toLowerCase().includes('zh') ? 'zh-TW' : 'vi';

  return (
    <Select value={currentLang} onValueChange={changeLanguage}>
      <SidebarMenuButton
        tooltip="Ngôn ngữ / 語言"
        render={
          <SelectTrigger className="border-none shadow-none focus:ring-0 focus-visible:ring-0 focus-visible:ring-offset-0 bg-transparent px-2 w-full justify-start py-0 h-auto gap-2" />
        }
      >
        <Globe className="size-4 shrink-0" />
        <span className="flex-1 text-left">{currentLang === 'zh-TW' ? '繁體中文' : 'Tiếng Việt'}</span>
      </SidebarMenuButton>
      <SelectContent side="top" align="center">
        <SelectItem value="vi">Tiếng Việt</SelectItem>
        <SelectItem value="zh-TW">繁體中文</SelectItem>
      </SelectContent>
    </Select>
  );
}
