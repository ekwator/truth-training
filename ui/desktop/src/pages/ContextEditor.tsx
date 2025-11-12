import React, { useState, useEffect } from 'react';
import { ApiService } from '@/services/api';
import { useToast } from '@/components/system/Toaster';
import { CreateContextRequest } from '@/types/contexts';
import { useContextEditorStore } from '@/stores/contextEditor';

interface ContextEditorProps {
  eventId?: number;
  prefilledData?: {
    category_id?: number;
    forma_id?: number;
    cause_id?: number;
    develop_id?: number;
    effect_id?: number;
    name?: string;
  };
  onClose?: () => void;
}

export const ContextEditor: React.FC<ContextEditorProps> = ({ eventId, prefilledData, onClose }) => {
  const { addToast } = useToast();
  const { prefilledData: storePrefilledData, setPrefilledData } = useContextEditorStore();
  const [loading, setLoading] = useState(false);
  const [duplicateError, setDuplicateError] = useState<string | null>(null);
  const [formData, setFormData] = useState<CreateContextRequest>({
    name: '',
    category_id: undefined,
    forma_id: undefined,
    cause_id: undefined,
    develop_id: undefined,
    effect_id: undefined,
    description: '',
  });

  // Prefill from props or store (when creating from event)
  useEffect(() => {
    // Clear store prefilled data after using it
    if (storePrefilledData) {
      setFormData({
        name: storePrefilledData.name || '',
        category_id: storePrefilledData.category_id,
        forma_id: storePrefilledData.forma_id,
        cause_id: storePrefilledData.cause_id,
        develop_id: storePrefilledData.develop_id,
        effect_id: storePrefilledData.effect_id,
        description: '',
      });
      setPrefilledData(null); // Clear after using
    } else if (eventId !== undefined) {
      // Fetch event to get embedded fields
      ApiService.getEvent(eventId)
        .then((event) => {
          setFormData({
            name: prefilledData?.name || '',
            category_id: event.category_id,
            forma_id: event.forma_id,
            cause_id: event.cause_id,
            develop_id: event.develop_id,
            effect_id: event.effect_id,
            description: '',
          });
        })
        .catch((error) => {
          console.error('Failed to fetch event:', error);
          addToast({
            type: 'error',
            title: 'Failed to Load Event',
            message: 'Could not load event data for template creation.'
          });
        });
    } else if (prefilledData) {
      // Prefill from props directly
      setFormData({
        name: prefilledData.name || '',
        category_id: prefilledData.category_id,
        forma_id: prefilledData.forma_id,
        cause_id: prefilledData.cause_id,
        develop_id: prefilledData.develop_id,
        effect_id: prefilledData.effect_id,
        description: '',
      });
    }
  }, [eventId, prefilledData, storePrefilledData, setPrefilledData, addToast]);

  const handleChange = (field: keyof CreateContextRequest, value: string | number | undefined) => {
    setFormData({ ...formData, [field]: value });
    // Clear duplicate error when user modifies fields
    if (duplicateError) {
      setDuplicateError(null);
    }
  };

  const handleSave = async () => {
    if (!formData.name.trim()) {
      addToast({
        type: 'error',
        title: 'Validation Error',
        message: 'Template name is required.'
      });
      return;
    }

    setLoading(true);
    setDuplicateError(null);

    try {
      await ApiService.createContext(formData);
      addToast({
        type: 'success',
        title: 'Template Created',
        message: `Context template "${formData.name}" has been created successfully.`
      });
      handleClear();
      // Call onClose if provided, otherwise stay on page
      if (onClose) {
        onClose();
      }
    } catch (error: any) {
      console.error('Failed to create context template:', error);
      const errorMessage = error?.response?.data || error?.message || 'Unknown error';
      
      // Check for duplicate error (409 Conflict)
      if (error?.response?.status === 409 || errorMessage.includes('already exists') || errorMessage.includes('duplicate')) {
        setDuplicateError('Template already exists. A template with identical non-NULL fields already exists. Please modify the fields or use the existing template.');
        addToast({
          type: 'error',
          title: 'Duplicate Template',
          message: 'A template with identical non-NULL fields already exists.'
        });
      } else if (error?.response?.status === 400 || errorMessage.includes('Foreign key') || errorMessage.includes('does not exist')) {
        addToast({
          type: 'error',
          title: 'Validation Error',
          message: 'One or more foreign key references are invalid. Please check the IDs and try again.'
        });
      } else {
        addToast({
          type: 'error',
          title: 'Failed to Create Template',
          message: errorMessage || 'Please check your connection and try again.'
        });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setDuplicateError(null);
    setFormData({
      name: '',
      category_id: undefined,
      forma_id: undefined,
      cause_id: undefined,
      develop_id: undefined,
      effect_id: undefined,
      description: '',
    });
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-3xl mx-auto px-4">
        <h1 className="text-3xl font-bold mb-6">Context Template Editor</h1>

        <div className="bg-white rounded-lg shadow p-6 space-y-6">
          {duplicateError && (
            <div className="px-4 py-3 bg-red-50 border border-red-200 rounded text-sm text-red-800">
              <strong>Error:</strong> {duplicateError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium mb-2">Template Name *</label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => handleChange('name', e.target.value)}
              className="w-full px-3 py-2 border rounded"
              placeholder="Enter template name (e.g., 'Interpersonal Conflict')"
              required
            />
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

          <div>
            <label className="block text-sm font-medium mb-2">Description (Optional)</label>
            <textarea
              value={formData.description || ''}
              onChange={(e) => handleChange('description', e.target.value)}
              className="w-full px-3 py-2 border rounded"
              rows={4}
              placeholder="Enter template description..."
            />
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded p-3 text-sm text-blue-800">
            <strong>Note:</strong> Duplicate detection compares only non-NULL fields. Templates with identical non-NULL field values cannot be created.
          </div>

          <div className="flex gap-2">
            <button
              onClick={handleSave}
              disabled={loading}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Template'}
            </button>
            <button
              onClick={handleClear}
              className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
            >
              Clear Form
            </button>
            {onClose && (
              <button
                onClick={onClose}
                className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700"
              >
                Close
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

