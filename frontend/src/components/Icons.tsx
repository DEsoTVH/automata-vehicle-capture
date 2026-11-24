import type { ReactNode } from 'react';

function Svg({ children, size = 22 }: { children: ReactNode; size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.9}
         strokeLinecap="round" strokeLinejoin="round" aria-hidden focusable="false">
      {children}
    </svg>
  );
}

type P = { size?: number };

export const IconHome = (p: P) => (
  <Svg {...p}><path d="M3 11l9-7 9 7" /><path d="M5 10v10h14V10" /><path d="M10 20v-6h4v6" /></Svg>
);
export const IconCamera = (p: P) => (
  <Svg {...p}><path d="M4 8h3l2-3h6l2 3h3v11H4z" /><circle cx="12" cy="13" r="3.5" /></Svg>
);
export const IconCar = (p: P) => (
  <Svg {...p}><path d="M5 16l1.5-5.5A2 2 0 0 1 8.4 9h7.2a2 2 0 0 1 1.9 1.5L19 16" /><path d="M3 16h18v3H3z" /><circle cx="7.5" cy="19" r="0.6" /><circle cx="16.5" cy="19" r="0.6" /></Svg>
);
export const IconExport = (p: P) => (
  <Svg {...p}><path d="M12 3v12" /><path d="M7 11l5 5 5-5" /><path d="M4 20h16" /></Svg>
);
export const IconImage = (p: P) => (
  <Svg {...p}><rect x="3" y="4" width="18" height="16" rx="1" /><circle cx="9" cy="10" r="1.6" /><path d="M21 16l-5-5-8 9" /></Svg>
);
export const IconClose = (p: P) => (
  <Svg {...p}><path d="M6 6l12 12M18 6L6 18" /></Svg>
);
export const IconAlert = (p: P) => (
  <Svg {...p}><path d="M12 3l10 18H2z" /><path d="M12 10v5" /><path d="M12 18.2v.1" /></Svg>
);
export const IconArrow = (p: P) => (
  <Svg {...p}><path d="M5 12h14" /><path d="M13 6l6 6-6 6" /></Svg>
);
