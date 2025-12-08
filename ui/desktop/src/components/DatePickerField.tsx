import React, { useState, useRef, useEffect } from 'react';
import { normalizeToStartOfDay } from '@/utils/dateNormalization';
import { t } from '@/i18n';

export interface DatePickerFieldProps {
  value?: number | null; // Unix timestamp in seconds
  onChange: (value: number | null) => void;
  label: string;
  placeholder?: string;
  error?: string;
  disabled?: boolean;
  required?: boolean;
  allowClear?: boolean;
  minDate?: number; // Unix timestamp in seconds
  maxDate?: number; // Unix timestamp in seconds
}

/**
 * DatePickerField component matching Android DatePickerField behavior.
 * 
 * Features:
 * - Date normalization to start of day
 * - Validation rules (min/max date)
 * - Clear capability (if allowClear = true)
 * - Locale-aware date formatting
 */
export const DatePickerField: React.FC<DatePickerFieldProps> = ({
  value,
  onChange,
  label,
  placeholder,
  error,
  disabled = false,
  required = false,
  allowClear = false,
  minDate,
  maxDate,
}) => {
  const [displayValue, setDisplayValue] = useState<string>('');
  const [validationError, setValidationError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Update display value when value prop changes
  useEffect(() => {
    if (value) {
      const date = new Date(value * 1000);
      // Format as YYYY-MM-DD for datetime-local input
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      setDisplayValue(`${year}-${month}-${day}`);
    } else {
      setDisplayValue('');
    }
  }, [value]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const inputValue = e.target.value;
    setDisplayValue(inputValue);
    setValidationError(null);

    if (!inputValue) {
      onChange(null);
      if (required) {
        setValidationError(t('validation.dateRequired'));
      }
      return;
    }

    try {
      const date = new Date(inputValue);
      if (isNaN(date.getTime())) {
        setValidationError(t('validation.invalidDate'));
        return;
      }

      // Normalize to start of day
      const normalizedDate = normalizeToStartOfDay(date);
      const timestamp = Math.floor(normalizedDate.getTime() / 1000);

      // Validate min date
      if (minDate !== undefined && timestamp < minDate) {
        setValidationError(t('validation.dateTooEarly'));
        return;
      }

      // Validate max date
      if (maxDate !== undefined && timestamp > maxDate) {
        setValidationError(t('validation.dateTooLate'));
        return;
      }

      onChange(timestamp);
    } catch (err) {
      setValidationError(t('validation.invalidDate'));
    }
  };

  const handleClear = () => {
    setDisplayValue('');
    setValidationError(null);
    onChange(null);
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };

  const hasError = error || validationError;

  return (
    <div className="relative">
      <label className="block text-sm font-medium text-gray-700 mb-2">
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
      </label>

      <div className="relative">
        <input
          ref={inputRef}
          type="date"
          value={displayValue}
          onChange={handleChange}
          placeholder={placeholder}
          disabled={disabled}
          min={minDate ? new Date(minDate * 1000).toISOString().split('T')[0] : undefined}
          max={maxDate ? new Date(maxDate * 1000).toISOString().split('T')[0] : undefined}
          className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
            hasError ? 'border-red-500' : 'border-gray-300'
          } ${disabled ? 'bg-gray-100 cursor-not-allowed' : ''}`}
        />
        {allowClear && value && !disabled && (
          <button
            type="button"
            onClick={handleClear}
            className="absolute right-2 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
            aria-label={t('common.clear')}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>

      {/* Error message */}
      {hasError && (
        <p className="mt-1 text-sm text-red-600">{error || validationError}</p>
      )}

      {/* Helper text */}
      {!hasError && value && (
        <p className="mt-1 text-xs text-gray-500">
          {new Date(value * 1000).toLocaleDateString()}
        </p>
      )}
    </div>
  );
};

