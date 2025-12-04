import React, { useState, useEffect, useRef, useCallback } from 'react';
import { ApiService } from '@/services/api';
import { ContextTemplate } from '@/types/contexts';

export interface ContextPickerProps {
  value?: number;
  onChange: (value: number | undefined) => void;
  label: string;
  placeholder?: string;
  error?: string;
  disabled?: boolean;
  required?: boolean;
}

export const ContextPicker: React.FC<ContextPickerProps> = ({
  value,
  onChange,
  label,
  placeholder = 'Select or enter context ID',
  error,
  disabled = false,
  required = false,
}) => {
  const [contexts, setContexts] = useState<ContextTemplate[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorState, setErrorState] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [fetchedAt, setFetchedAt] = useState<string | null>(null);
  const [isStale, setIsStale] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Check if data is stale (>24h)
  const checkStaleness = useCallback((fetchedAtStr: string) => {
    const fetched = new Date(fetchedAtStr);
    const now = new Date();
    const hoursDiff = (now.getTime() - fetched.getTime()) / (1000 * 60 * 60);
    return hoursDiff > 24;
  }, []);

  // Load contexts from API with cache fallback
  const loadContexts = useCallback(async (useCache: boolean = true) => {
    setLoading(true);
    setErrorState(null);
    try {
      const response = await ApiService.getContexts(useCache);
      setContexts(response.data || []);
      setFetchedAt(response.fetched_at);
      setIsStale(checkStaleness(response.fetched_at));
      
      // Emit telemetry: context_picker.load.success
      console.log('context_picker.load.success', {
        count: response.data?.length || 0,
        fetched_at: response.fetched_at,
        from_cache: useCache,
      });
    } catch (err) {
      // Try to use cached data as fallback
      if (useCache && typeof window !== 'undefined' && window.localStorage) {
        try {
          const cached = localStorage.getItem('truth_contexts_cache');
          if (cached) {
            const parsed = JSON.parse(cached);
            setContexts(parsed.data || []);
            setFetchedAt(parsed.fetched_at);
            setIsStale(checkStaleness(parsed.fetched_at));
            
            // Show offline banner but allow using cached data
            setErrorState('Using cached data. Connection failed. Retry to refresh.');
            
            // Emit telemetry: context_picker.load.failure with cache fallback
            console.warn('context_picker.load.failure', { 
              error: err,
              fallback_to_cache: true,
            });
            setLoading(false);
            return;
          }
        } catch (cacheErr) {
          // Cache read failed too
        }
      }
      
      const errorMsg = 'Unable to load contexts. Retry';
      setErrorState(errorMsg);
      
      // Emit telemetry: context_picker.load.failure
      console.error('context_picker.load.failure', { error: err });
    } finally {
      setLoading(false);
    }
  }, [checkStaleness]);

  useEffect(() => {
    loadContexts();
  }, [loadContexts]);

  // Validate manual entry against dataset
  const validateValue = useCallback((id: number | undefined): boolean => {
    if (id === undefined) {
      return !required;
    }
    return contexts.some((ctx) => ctx.id === id);
  }, [contexts, required]);

  // Handle manual input change
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const inputValue = e.target.value.trim();
    setSearchQuery(inputValue);
    
    if (inputValue === '') {
      onChange(undefined);
      return;
    }

    const numValue = parseInt(inputValue, 10);
    if (!isNaN(numValue)) {
      if (validateValue(numValue)) {
        onChange(numValue);
        setErrorState(null);
      } else {
        // Invalid ID - block submission, show error
        setErrorState('Unknown context ID');
        
        // Emit telemetry: context_picker.validation.failure
        console.warn('context_picker.validation.failure', {
          invalid_id: numValue,
          available_ids: contexts.map((c) => c.id),
        });
      }
    }
  };

  // Handle selection from dropdown
  const handleSelect = (context: ContextTemplate) => {
    onChange(context.id);
    setSearchQuery(context.name);
    setIsOpen(false);
    setErrorState(null);
  };

  // Filter contexts by search query
  const filteredContexts = contexts.filter((ctx) =>
    ctx.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    ctx.id.toString().includes(searchQuery)
  );

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Find selected context for display
  const selectedContext = value ? contexts.find((c) => c.id === value) : null;

  return (
    <div className="relative" ref={containerRef}>
      <label className="block text-sm font-medium mb-2">
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
      </label>
      
      {/* Stale data warning */}
      {isStale && fetchedAt && (
        <div className="mb-2 px-3 py-2 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-800">
          Data is stale (last updated: {new Date(fetchedAt).toLocaleString()}). Please refresh.
        </div>
      )}

      {/* Error state */}
      {errorState && (
        <div className="mb-2 px-3 py-2 bg-red-50 border border-red-200 rounded text-sm text-red-800">
          {errorState}
        </div>
      )}

      {/* Loading state */}
      {loading && (
        <div className="mb-2 px-3 py-2 bg-gray-50 border border-gray-200 rounded text-sm text-gray-600">
          Loading contexts...
        </div>
      )}

      {/* Retry button on error */}
      {errorState && !loading && (
        <button
          type="button"
          onClick={() => loadContexts(false)}
          className="mb-2 px-3 py-1 text-sm bg-blue-600 text-white rounded hover:bg-blue-700"
        >
          Retry
        </button>
      )}

      {/* Combobox input */}
      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={searchQuery || (selectedContext ? selectedContext.name : '') || value?.toString() || ''}
          onChange={handleInputChange}
          onFocus={() => setIsOpen(true)}
          placeholder={placeholder}
          disabled={disabled || loading}
          className={`w-full px-3 py-2 border rounded ${
            errorState || error ? 'border-red-500' : 'border-gray-300'
          } ${disabled ? 'bg-gray-100 cursor-not-allowed' : ''}`}
          aria-expanded={isOpen}
          aria-haspopup="listbox"
          role="combobox"
        />

        {/* Dropdown */}
        {isOpen && !loading && filteredContexts.length > 0 && (
          <ul
            className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded shadow-lg max-h-60 overflow-auto"
            role="listbox"
          >
            {filteredContexts.map((ctx) => (
              <li
                key={ctx.id}
                onClick={() => handleSelect(ctx)}
                className={`px-3 py-2 cursor-pointer hover:bg-gray-100 ${
                  value === ctx.id ? 'bg-blue-50' : ''
                }`}
                role="option"
                aria-selected={value === ctx.id}
              >
                <div className="font-medium">{ctx.name}</div>
                {ctx.description && (
                  <div className="text-sm text-gray-500">{ctx.description}</div>
                )}
                <div className="text-xs text-gray-400">ID: {ctx.id}</div>
              </li>
            ))}
          </ul>
        )}

        {/* Empty state */}
        {!loading && !errorState && contexts.length === 0 && (
          <div className="mt-2 px-3 py-2 bg-gray-50 border border-gray-200 rounded text-sm text-gray-600">
            No contexts available. Please seed contexts in the database.
          </div>
        )}
      </div>

      {/* Helper text */}
      {!errorState && !loading && (
        <p className="mt-1 text-xs text-gray-500">
          {fetchedAt
            ? `Last updated: ${new Date(fetchedAt).toLocaleString()}`
            : 'Type to search or enter a context ID'}
        </p>
      )}
    </div>
  );
};

