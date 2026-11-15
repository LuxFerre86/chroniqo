import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

// Imported lazily: a static import here would load the real api modules before a test file's vi.mock() applies.
afterEach(async () => {
  cleanup();
  const { setActiveLanguage } = await import('../i18n');
  setActiveLanguage('en');
});
