import React, { useState, useEffect, useCallback } from 'react';
import { ApiService } from '@/services/api';

interface EventDisplay {
  id: string;
  title: string;
  description: string;
  context?: string;
  results?: string;
  notes?: string;
  recommendations?: string;
  createdAt?: string;
  updatedAt?: string;
}

export const EventSummary: React.FC = () => {
  const [events, setEvents] = useState<any[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<EventDisplay | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    description: '',
    results: '',
    notes: '',
    recommendations: '',
  });

  const fetchEvents = useCallback(async () => {
    try {
      const data = await ApiService.getEvents(1, 100);
      setEvents(data.data || []);
    } catch (error) {
      console.error('Failed to fetch events:', error);
    }
  }, []);

  const fetchEventDetails = useCallback(async (id: string) => {
    try {
      const event = await ApiService.getEvent(id);
      const display: EventDisplay = {
        id: event.id,
        title: event.title,
        description: event.description,
        results: (event as any).results,
        notes: (event as any).notes,
        recommendations: (event as any).recommendations,
        createdAt: event.created_at,
        updatedAt: event.updated_at,
      };
      setSelectedEvent(display);
      setFormData({
        description: event.description || '',
        results: (event as any).results || '',
        notes: (event as any).notes || '',
        recommendations: (event as any).recommendations || '',
      });
    } catch (error) {
      console.error('Failed to fetch event details:', error);
    }
  }, []);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  const handleSelectEvent = (event: any) => {
    fetchEventDetails(event.id);
  };

  const handleSaveImpact = async () => {
    if (!selectedEvent) return;
    alert('Add Impact will be implemented via Tauri command');
  };

  const handleSaveChanges = async () => {
    if (!selectedEvent) return;
    console.log('Saving changes:', formData);
    setIsEditing(false);
    alert('Save Changes will be implemented via Tauri command');
  };

  if (selectedEvent) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-3xl mx-auto px-4">
          <button
            onClick={() => setSelectedEvent(null)}
            className="mb-4 px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
          >
            Back to Event List
          </button>

          <div className="bg-white rounded-lg shadow p-6">
            <h1 className="text-2xl font-bold mb-4">{selectedEvent.title}</h1>

            {isEditing ? (
              <>
                <div className="mb-4">
                  <label className="block text-sm font-medium mb-2">Description</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-3 py-2 border rounded"
                    rows={3}
                  />
                </div>
                <div className="mb-4">
                  <label className="block text-sm font-medium mb-2">Results (Impact)</label>
                  <textarea
                    value={formData.results}
                    onChange={(e) => setFormData({ ...formData, results: e.target.value })}
                    className="w-full px-3 py-2 border rounded"
                    rows={3}
                  />
                </div>
                <div className="mb-4">
                  <label className="block text-sm font-medium mb-2">Notes</label>
                  <textarea
                    value={formData.notes}
                    onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                    className="w-full px-3 py-2 border rounded"
                    rows={3}
                  />
                </div>
                <div className="mb-4">
                  <label className="block text-sm font-medium mb-2">Recommendations</label>
                  <textarea
                    value={formData.recommendations}
                    onChange={(e) => setFormData({ ...formData, recommendations: e.target.value })}
                    className="w-full px-3 py-2 border rounded"
                    rows={3}
                  />
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={handleSaveChanges}
                    className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
                  >
                    Save Changes
                  </button>
                  <button
                    onClick={() => setIsEditing(false)}
                    className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
                  >
                    Cancel
                  </button>
                </div>
              </>
            ) : (
              <>
                <div className="mb-4">
                  <p className="font-medium">Description</p>
                  <p className="text-gray-600">{selectedEvent.description}</p>
                </div>
                <div className="mb-4">
                  <p className="font-medium">Results (Impact)</p>
                  <p className="text-gray-600">{selectedEvent.results || 'Not specified'}</p>
                </div>
                <div className="mb-4">
                  <p className="font-medium">Notes</p>
                  <p className="text-gray-600">{selectedEvent.notes || 'No notes'}</p>
                </div>
                <div className="mb-4">
                  <p className="font-medium">Recommendations</p>
                  <p className="text-gray-600">{selectedEvent.recommendations || 'No recommendations'}</p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={handleSaveImpact}
                    className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
                  >
                    Add Impact
                  </button>
                  <button
                    onClick={() => setIsEditing(true)}
                    className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
                  >
                    Edit Summary
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4">
        <h1 className="text-3xl font-bold mb-6">Event Summary</h1>

        {events.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-12 text-center">
            <div className="text-gray-400 mb-4 text-sm">No data</div>
            <p className="text-gray-600">No events available.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {events.map((event) => (
              <div
                key={event.id}
                onClick={() => handleSelectEvent(event)}
                className="bg-white rounded-lg shadow p-4 hover:bg-gray-50 cursor-pointer"
              >
                <h3 className="text-lg font-semibold">{event.title}</h3>
                <p className="text-sm text-gray-600">{event.description || 'No description'}</p>
                <p className="text-xs text-gray-500 mt-2">Created: {event.created_at ? new Date(event.created_at).toLocaleString() : 'Unknown'}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
