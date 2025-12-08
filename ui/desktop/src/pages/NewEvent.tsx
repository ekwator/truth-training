import React, { useState, useEffect, useCallback } from 'react';
import { ApiService } from '@/services/api';
import { useToast } from '@/components/system/Toaster';
import { ContextTemplate } from '@/types/contexts';
import { ContextPicker } from '@/components/context/ContextPicker';
import { useNavigationStore } from '@/stores/navigation';
import { useTemplateContextStore } from '@/stores/templateContext';
import { Screen } from '@/components/layout/TopMenuBar';
import { t } from '@/i18n';
import { validateEvent } from '@/utils/validation';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface NewEventProps {
  onNavigate?: (screen: Screen, state?: NavigationState) => void;
}

export const NewEvent: React.FC<NewEventProps> = ({ onNavigate }) => {
  const { addToast } = useToast();
  const { setSelectTemplateForEvent } = useNavigationStore();
  const { selectedTemplateContext, setSelectedTemplateContext } = useTemplateContextStore();
  const [templates, setTemplates] = useState<ContextTemplate[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingTemplates, setLoadingTemplates] = useState(false);
  const [confessionMode, setConfessionMode] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<string>('');
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  // Suppress unused warnings - reserved for future use
  // @ts-ignore
  void loadingTemplates;
  // @ts-ignore
  void selectedTemplate;
  const [formData, setFormData] = useState({
    event_name: '',
    description: '',
    vector: true,
    category_id: undefined as number | undefined,
    forma_id: undefined as number | undefined,
    cause_id: undefined as number | undefined,
    develop_id: undefined as number | undefined,
    effect_id: undefined as number | undefined,
    start_date: '',
    end_date: '',
  });

  const fetchTemplates = useCallback(async () => {
    setLoadingTemplates(true);
    try {
      const response = await ApiService.getContexts();
      setTemplates(response.data || []);
    } catch (error) {
      console.error('Failed to fetch context templates:', error);
      // Don't show error toast, templates are optional
    } finally {
      setLoadingTemplates(false);
    }
  }, []);

  useEffect(() => {
    fetchTemplates();
  }, [fetchTemplates]);

  // Prefill form from selected template context (when returning from template selection)
  useEffect(() => {
    if (selectedTemplateContext) {
      setFormData(prev => ({
        ...prev,
        category_id: selectedTemplateContext.categoryId || undefined,
        forma_id: selectedTemplateContext.formaId || undefined,
        cause_id: selectedTemplateContext.causeId || undefined,
        develop_id: selectedTemplateContext.developId || undefined,
        effect_id: selectedTemplateContext.effectId || undefined,
      }));
      // Clear template context after using
      setSelectedTemplateContext(null);
    }
  }, [selectedTemplateContext, setSelectedTemplateContext]);

  // Handle template selection button click
  const handleSelectTemplate = () => {
    setSelectTemplateForEvent(true);
    onNavigate?.('context-editor');
  };

  const handleChange = (field: string, value: string | number | boolean | undefined) => {
    setFormData({ ...formData, [field]: value });
  };

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const handleTemplateSelect = (templateId: string) => {
    setSelectedTemplate(templateId);
    if (templateId) {
      const template = templates.find(t => t.id.toString() === templateId);
      if (template) {
        // Prefill fields from template (user can still modify)
        setFormData({
          ...formData,
          category_id: template.category_id,
          forma_id: template.forma_id,
          cause_id: template.cause_id,
          develop_id: template.develop_id,
          effect_id: template.effect_id,
        });
        addToast({
          type: 'info',
          title: 'Template Selected',
          message: `Fields prefilled from template "${template.name}". You can modify them before saving.`
        });
      }
    } else {
      // Clear template selection
      setFormData({
        ...formData,
        category_id: undefined,
        forma_id: undefined,
        cause_id: undefined,
        develop_id: undefined,
        effect_id: undefined,
      });
    }
  };

  const handleSave = async () => {
    // Clear previous validation errors
    setValidationErrors({});
    
    // In confession mode, auto-derive a minimal title if empty
    let eventName = formData.event_name.trim();
    if (confessionMode && !eventName && formData.description.trim()) {
      eventName = formData.description.trim().slice(0, 40) || 'Confession';
    }

    const description = formData.description.trim() || eventName;

    // Convert date strings to timestamps for validation
    const timestampStart = formData.start_date 
      ? Math.floor(new Date(formData.start_date).getTime() / 1000)
      : Math.floor(Date.now() / 1000);
    const timestampEnd = formData.end_date 
      ? Math.floor(new Date(formData.end_date).getTime() / 1000)
      : null;

    // Validate using validation utility
    const validationResult = validateEvent({
      name: eventName,
      description: description,
      categoryId: formData.category_id ?? null,
      formaId: formData.forma_id ?? null,
      causeId: formData.cause_id ?? null,
      developId: formData.develop_id ?? null,
      effectId: formData.effect_id ?? null,
      timestampStart: timestampStart,
      timestampEnd: timestampEnd,
    }, (key: string) => t(key));

    if (!validationResult.isValid) {
      // Map validation errors to form fields
      const errors: Record<string, string> = {};
      validationResult.errors.forEach(error => {
        errors[error.field] = error.message;
      });
      setValidationErrors(errors);
      
      // Show first error in toast
      if (validationResult.errors.length > 0) {
        addToast({
          type: 'error',
          title: t('validation.validationError'),
          message: validationResult.errors[0].message
        });
      }
      return;
    }

    setLoading(true);
    try {
      await ApiService.createEvent({
        description,
        category_id: formData.category_id,
        forma_id: formData.forma_id,
        cause_id: formData.cause_id,
        develop_id: formData.develop_id,
        effect_id: formData.effect_id,
        vector: formData.vector
      });
      addToast({
        type: 'success',
        title: 'Event Created',
        message: 'The event has been created successfully.'
      });
      handleClear();
    } catch (error) {
      console.error('Failed to create event:', error);
      addToast({
        type: 'error',
        title: 'Failed to Create Event',
        message: 'Please check your connection and try again.'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setSelectedTemplate('');
    setFormData({
      event_name: '',
      description: '',
      vector: true,
      category_id: undefined,
      forma_id: undefined,
      cause_id: undefined,
      develop_id: undefined,
      effect_id: undefined,
      start_date: '',
      end_date: '',
    });
  };

  const handleGoToSummary = () => {
    // TODO: Navigate to Event Summary (wire to navigation)
    alert('Navigate to Event Summary (TBA via parent navigation)');
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-3xl mx-auto px-4">
        <h1 className="text-3xl font-bold mb-6">Create New Event</h1>

        <div className="bg-white rounded-lg shadow p-6 space-y-6">
          <div className="flex items-center justify-between">
            <label className="inline-flex items-center space-x-2">
              <input
                type="checkbox"
                checked={confessionMode}
                onChange={(e) => setConfessionMode(e.target.checked)}
              />
              <span className="text-sm font-medium">Anonymous Confession Mode</span>
            </label>
          </div>

          {confessionMode && (
            <div className="px-3 py-2 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-800">
              Anonymous confessions are stored plaintext-at-rest. Do not include sensitive identifiers. Title may be auto-derived from description.
            </div>
          )}

          <div>
            <label htmlFor="event-name" className="block text-sm font-medium mb-2">Event Name *</label>
            <input
              id="event-name"
              type="text"
              value={formData.event_name}
              onChange={(e) => {
                handleChange('event_name', e.target.value);
                // Clear validation error when user types
                if (validationErrors.name) {
                  setValidationErrors(prev => {
                    const newErrors = { ...prev };
                    delete newErrors.name;
                    return newErrors;
                  });
                }
              }}
              className={`w-full px-3 py-2 border rounded ${
                validationErrors.name ? 'border-red-500' : ''
              }`}
              required
            />
            {validationErrors.name && (
              <p className="mt-1 text-sm text-red-600">{validationErrors.name}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">Description</label>
            <textarea
              value={formData.description}
              onChange={(e) => {
                handleChange('description', e.target.value);
                // Clear validation error when user types
                if (validationErrors.description) {
                  setValidationErrors(prev => {
                    const newErrors = { ...prev };
                    delete newErrors.description;
                    return newErrors;
                  });
                }
              }}
              className={`w-full px-3 py-2 border rounded ${
                validationErrors.description ? 'border-red-500' : ''
              }`}
              rows={4}
            />
            {validationErrors.description && (
              <p className="mt-1 text-sm text-red-600">{validationErrors.description}</p>
            )}
            {confessionMode && !validationErrors.description && (
              <p className="mt-1 text-xs text-gray-500">
                Description is optional in confession mode; if left blank, the event name will be used.
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">Vector</label>
            <select
              value={formData.vector ? 'outgoing' : 'incoming'}
              onChange={(e) => handleChange('vector', e.target.value === 'outgoing')}
              className="w-full px-3 py-2 border rounded"
            >
              <option value="outgoing">Outgoing (from user)</option>
              <option value="incoming">Incoming (to user)</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">{t('events.contextTemplate')}</label>
            <div className="flex items-center space-x-2">
              <button
                type="button"
                onClick={handleSelectTemplate}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                {t('events.selectTemplate')}
              </button>
              {selectedTemplateContext && (
                <span className="text-sm text-gray-600">{t('events.templateSelected')}</span>
              )}
            </div>
            <p className="mt-1 text-xs text-gray-500">
              {t('events.templateSelectionHint')}
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <ContextPicker
              label="Category"
              value={formData.category_id}
              onChange={(value) => {
                handleChange('category_id', value);
                if (validationErrors.categoryId) {
                  setValidationErrors(prev => {
                    const newErrors = { ...prev };
                    delete newErrors.categoryId;
                    return newErrors;
                  });
                }
              }}
              placeholder="Select or enter category ID"
              error={validationErrors.categoryId}
              required
            />
            <ContextPicker
              label="Forma"
              value={formData.forma_id}
              onChange={(value) => {
                handleChange('forma_id', value);
                if (validationErrors.formaId) {
                  setValidationErrors(prev => {
                    const newErrors = { ...prev };
                    delete newErrors.formaId;
                    return newErrors;
                  });
                }
              }}
              placeholder="Select or enter forma ID"
              error={validationErrors.formaId}
              required
            />
            <ContextPicker
              label="Cause"
              value={formData.cause_id}
              onChange={(value) => {
                handleChange('cause_id', value);
                if (validationErrors.causeId) {
                  setValidationErrors(prev => {
                    const newErrors = { ...prev };
                    delete newErrors.causeId;
                    return newErrors;
                  });
                }
              }}
              placeholder="Select or enter cause ID"
              error={validationErrors.causeId}
              required
            />
            <ContextPicker
              label="Develop"
              value={formData.develop_id}
              onChange={(value) => {
                handleChange('develop_id', value);
                if (validationErrors.developId) {
                  setValidationErrors(prev => {
                    const newErrors = { ...prev };
                    delete newErrors.developId;
                    return newErrors;
                  });
                }
              }}
              placeholder="Select or enter develop ID"
              error={validationErrors.developId}
              required
            />
            <ContextPicker
              label="Effect"
              value={formData.effect_id}
              onChange={(value) => {
                handleChange('effect_id', value);
                if (validationErrors.effectId) {
                  setValidationErrors(prev => {
                    const newErrors = { ...prev };
                    delete newErrors.effectId;
                    return newErrors;
                  });
                }
              }}
              placeholder="Select or enter effect ID"
              error={validationErrors.effectId}
              required
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-2">Start Date</label>
              <input
                type="date"
                value={formData.start_date}
                onChange={(e) => {
                  handleChange('start_date', e.target.value);
                  if (validationErrors.timestampStart || validationErrors.timestampEnd) {
                    setValidationErrors(prev => {
                      const newErrors = { ...prev };
                      delete newErrors.timestampStart;
                      delete newErrors.timestampEnd;
                      return newErrors;
                    });
                  }
                }}
                className={`w-full px-3 py-2 border rounded ${
                  validationErrors.timestampStart ? 'border-red-500' : ''
                }`}
              />
              {validationErrors.timestampStart && (
                <p className="mt-1 text-sm text-red-600">{validationErrors.timestampStart}</p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium mb-2">End Date</label>
              <input
                type="date"
                value={formData.end_date}
                onChange={(e) => {
                  handleChange('end_date', e.target.value);
                  if (validationErrors.timestampEnd) {
                    setValidationErrors(prev => {
                      const newErrors = { ...prev };
                      delete newErrors.timestampEnd;
                      return newErrors;
                    });
                  }
                }}
                className={`w-full px-3 py-2 border rounded ${
                  validationErrors.timestampEnd ? 'border-red-500' : ''
                }`}
              />
              {validationErrors.timestampEnd && (
                <p className="mt-1 text-sm text-red-600">{validationErrors.timestampEnd}</p>
              )}
            </div>
          </div>

          <div className="flex gap-2">
            <button
              onClick={handleSave}
              disabled={loading}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
            >
              Save Event
            </button>
            <button
              onClick={handleClear}
              className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
            >
              Clear Form
            </button>
            <button
              onClick={handleGoToSummary}
              className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
            >
              Go to Event Summary
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
