import React, { useState, useEffect, useCallback } from 'react';
import { ApiService } from '@/services/api';
import { useToast } from '@/components/system/Toaster';
import { ContextTemplate } from '@/types/contexts';

export const NewEvent: React.FC = () => {
  const { addToast } = useToast();
  const [templates, setTemplates] = useState<ContextTemplate[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingTemplates, setLoadingTemplates] = useState(false);
  const [confessionMode, setConfessionMode] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<string>('');
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

  const handleChange = (field: string, value: string | number | boolean | undefined) => {
    setFormData({ ...formData, [field]: value });
  };

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
    // In confession mode, auto-derive a minimal title if empty
    let eventName = formData.event_name.trim();
    if (confessionMode && !eventName && formData.description.trim()) {
      eventName = formData.description.trim().slice(0, 40) || 'Confession';
    }

    if (!eventName) {
      addToast({
        type: 'error',
        title: 'Validation Error',
        message: 'Event Name is required.'
      });
      return;
    }

    const description = formData.description.trim() || eventName;

    // Context fields are optional - no validation required

    if (formData.start_date && formData.end_date && formData.start_date > formData.end_date) {
      addToast({
        type: 'error',
        title: 'Validation Error',
        message: 'Start Date must be before End Date.'
      });
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
            <label className="block text-sm font-medium mb-2">Event Name *</label>
            <input
              type="text"
              value={formData.event_name}
              onChange={(e) => handleChange('event_name', e.target.value)}
              className="w-full px-3 py-2 border rounded"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">Description</label>
            <textarea
              value={formData.description}
              onChange={(e) => handleChange('description', e.target.value)}
              className="w-full px-3 py-2 border rounded"
              rows={4}
            />
            {confessionMode && (
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
            <label className="block text-sm font-medium mb-2">Context Template (Optional)</label>
            {loadingTemplates ? (
              <div className="px-3 py-2 bg-gray-50 border border-gray-200 rounded text-sm text-gray-600">
                Loading templates...
              </div>
            ) : templates.length === 0 ? (
              <div className="px-3 py-2 bg-gray-50 border border-gray-200 rounded text-sm text-gray-600">
                No templates available. You can enter context fields manually below.
              </div>
            ) : (
              <select
                value={selectedTemplate}
                onChange={(e) => handleTemplateSelect(e.target.value)}
                className="w-full px-3 py-2 border rounded"
              >
                <option value="">None - Enter fields manually</option>
                {templates.map((template) => (
                  <option key={template.id} value={template.id.toString()}>
                    {template.name}
                  </option>
                ))}
              </select>
            )}
            {selectedTemplate && (
              <p className="mt-2 text-xs text-gray-500">
                Fields prefilled from template. You can modify them below.
              </p>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-2">Category ID (Optional)</label>
              <input
                type="number"
                value={formData.category_id || ''}
                onChange={(e) => handleChange('category_id', e.target.value ? parseInt(e.target.value, 10) : undefined)}
                className="w-full px-3 py-2 border rounded"
                placeholder="Enter category ID"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-2">Forma ID (Optional)</label>
              <input
                type="number"
                value={formData.forma_id || ''}
                onChange={(e) => handleChange('forma_id', e.target.value ? parseInt(e.target.value, 10) : undefined)}
                className="w-full px-3 py-2 border rounded"
                placeholder="Enter forma ID"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-2">Cause ID (Optional)</label>
              <input
                type="number"
                value={formData.cause_id || ''}
                onChange={(e) => handleChange('cause_id', e.target.value ? parseInt(e.target.value, 10) : undefined)}
                className="w-full px-3 py-2 border rounded"
                placeholder="Enter cause ID"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-2">Develop ID (Optional)</label>
              <input
                type="number"
                value={formData.develop_id || ''}
                onChange={(e) => handleChange('develop_id', e.target.value ? parseInt(e.target.value, 10) : undefined)}
                className="w-full px-3 py-2 border rounded"
                placeholder="Enter develop ID"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-2">Effect ID (Optional)</label>
              <input
                type="number"
                value={formData.effect_id || ''}
                onChange={(e) => handleChange('effect_id', e.target.value ? parseInt(e.target.value, 10) : undefined)}
                className="w-full px-3 py-2 border rounded"
                placeholder="Enter effect ID"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-2">Start Date</label>
              <input
                type="date"
                value={formData.start_date}
                onChange={(e) => handleChange('start_date', e.target.value)}
                className="w-full px-3 py-2 border rounded"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-2">End Date</label>
              <input
                type="date"
                value={formData.end_date}
                onChange={(e) => handleChange('end_date', e.target.value)}
                className="w-full px-3 py-2 border rounded"
              />
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
