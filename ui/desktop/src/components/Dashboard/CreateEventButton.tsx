import React, { useState } from 'react';
import { useEventsStore } from '@/stores/events';

export const CreateEventButton: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [formData, setFormData] = useState({
    description: '',
    category_id: undefined as number | undefined,
    forma_id: undefined as number | undefined,
    cause_id: undefined as number | undefined,
    develop_id: undefined as number | undefined,
    effect_id: undefined as number | undefined,
    vector: true
  });
  const [loading, setLoading] = useState(false);
  const { createEvent } = useEventsStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.description.trim()) return;

    setLoading(true);
    try {
      await createEvent({
        description: formData.description.trim(),
        category_id: formData.category_id,
        forma_id: formData.forma_id,
        cause_id: formData.cause_id,
        develop_id: formData.develop_id,
        effect_id: formData.effect_id,
        vector: formData.vector
      });
      
      // Reset form and close modal
      setFormData({ description: '', category_id: undefined, forma_id: undefined, cause_id: undefined, develop_id: undefined, effect_id: undefined, vector: true });
      setIsOpen(false);
    } catch (error) {
      console.error('Failed to create event:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setFormData({ description: '', category_id: undefined, forma_id: undefined, cause_id: undefined, develop_id: undefined, effect_id: undefined, vector: true });
    setIsOpen(false);
  };

  return (
    <>
      <button
        onClick={() => setIsOpen(true)}
        className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
      >
        Create Event
      </button>

      {/* Modal */}
      {isOpen && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
          <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
            <div className="mt-3">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Create New Event</h3>
              
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                    Description *
                  </label>
                  <textarea
                    id="description"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    placeholder="Enter event description"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Vector
                  </label>
                  <select
                    value={formData.vector ? 'outgoing' : 'incoming'}
                    onChange={(e) => setFormData({ ...formData, vector: e.target.value === 'outgoing' })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="outgoing">Outgoing (from user)</option>
                    <option value="incoming">Incoming (to user)</option>
                  </select>
                </div>

                <div className="flex justify-end space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={handleCancel}
                    className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={loading || !formData.description.trim()}
                    className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
                  >
                    {loading ? 'Creating...' : 'Create Event'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
