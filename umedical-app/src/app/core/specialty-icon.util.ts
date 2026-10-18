const ICON_MAP: { keyword: string; icon: string }[] = [
  { keyword: 'pediatr', icon: 'happy-outline' },
  { keyword: 'cardio', icon: 'heart-outline' },
  { keyword: 'dermat', icon: 'body-outline' },
  { keyword: 'oftalm', icon: 'eye-outline' },
  { keyword: 'ocular', icon: 'eye-outline' },
  { keyword: 'general', icon: 'medical-outline' },
];

export function specialtyIcon(name: string): string {
  const n = name.toLowerCase();
  const match = ICON_MAP.find((m) => n.includes(m.keyword));
  return match?.icon ?? 'medical-outline';
}