import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import viTranslations from './locales/vi.json';
import twTranslations from './locales/zh-TW.json';

const resources = {
  vi: {
    translation: viTranslations,
  },
  'zh-TW': {
    translation: twTranslations,
  },
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next) // passes i18n down to react-i18next
  .init({
    resources,
    fallbackLng: 'vi', // use vi if translation is missing

    interpolation: {
      escapeValue: false, // react already safes from xss
    },
  });

export default i18n;
