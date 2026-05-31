import { Check } from 'lucide-react';
import { cn } from '@/lib/utils';

/**
 * Checkbox hiển thị (read-only) theo phong cách shadcn.
 * Dùng cho cột "Nội bộ": tick khi checked = true.
 */
export function Checkbox({ checked = false, className, ...props }) {
  return (
    <span
      data-slot="checkbox"
      data-state={checked ? 'checked' : 'unchecked'}
      className={cn(
        'inline-flex size-4 shrink-0 items-center justify-center rounded-[4px] border transition-colors',
        checked
          ? 'bg-emerald-500 border-emerald-500 text-white'
          : 'bg-background border-input',
        className
      )}
      {...props}
    >
      {checked && <Check className="size-3" strokeWidth={3} />}
    </span>
  );
}
