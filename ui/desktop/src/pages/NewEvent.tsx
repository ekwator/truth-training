import React, { useState, useEffect, useCallback } from 'react';
import { ApiService } from '@/services/api';
import { useToast } from '@/components/system/Toaster';

interface KnowledgeBaseItem {
  id: string;
  label: string;
}

export const NewEvent: React.FC = () => {
  const { addToast } = useToast();
  const [kbItems, setKbItems] = useState<KnowledgeBaseItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    event_name: '',
    description: '',
    context: '',
    start_date: '',
    end_date: '',
  });

  const fetchKnowledgeBase = useCallback(async () => {
    try {
      const items = await ApiService.getKnowledgeBaseItems();
      setKbItems(items || []);
    } catch (error) {
      console.error('Failed to fetch knowledge base:', error);
      addToast({
        type: 'error',
        title: 'Failed to load contexts',
        message: 'Please ensure docs/Data_Schema.md is available.'
      });
    }
  }, [addToast]);

  useEffect(() => {
    fetchKnowledgeBase();
  }, [fetchKnowledgeBase]);

  const handleChange = (field: string, value: string) => {
    setFormData({ ...formData, [field]: value });
  };

  const handleSave = async () => {
    if (!formData.event_name.trim()) {
      addToast({
        type: 'error',
        title: 'Validation Error',
        message: 'Event Name is required.'
      });
      return;
    }

    if (!formData.context) {
      addToast({
        type: 'error',
        title: 'Validation Error',
        message: 'Context selection is required.'
      });
      return;
    }

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
        title: formData.event_name,
        description: formData.description,
        context_id: formData.context,
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
    setFormData({
      event_name: '',
      description: '',
      context: '',
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
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">Context (Knowledge Base) *</label>
            {kbItems.length === 0 ? (
              <div className="px-3 py-2 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-800">
                No contexts available. Cannot save event.
              </div>
            ) : (
              <select
                value={formData.context}
                onChange={(e) => handleChange('context', e.target.value)}
                className="w-full px-3 py-2 border rounded"
                required
              >
                <option value="">Select a context...</option>
                {kbItems.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.label}
                  </option>
                ))}
              </select>
            )}
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
              disabled={loading || kbItems.length === 0}
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
