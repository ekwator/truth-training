/**
 * LocaleToggle component - DISABLED
 * English-only interface: Localization support has been removed.
 * This component is kept for compatibility but does not render anything.
 */
import React from 'react';

interface LocaleToggleProps {
  variant?: 'dropdown' | 'button';
  className?: string;
}

export const LocaleToggle: React.FC<LocaleToggleProps> = () => {
  // English-only interface - component disabled, returns null
  return null;
};
