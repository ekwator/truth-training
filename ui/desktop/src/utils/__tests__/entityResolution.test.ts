/**
 * Unit tests for entityResolution utility.
 * Tests getEntityNameById function matching Android behavior.
 */

import { getEntityNameById } from '../entityResolution';

describe('getEntityNameById', () => {
  interface TestEntity {
    id: number;
    name: string;
  }

  const testEntities: TestEntity[] = [
    { id: 1, name: 'Category A' },
    { id: 2, name: 'Category B' },
    { id: 3, name: 'Category C' },
  ];

  it('should return entity name when found', () => {
    const result = getEntityNameById(
      1,
      testEntities,
      (e) => e.id,
      (e) => e.name
    );
    expect(result).toBe('Category A');
  });

  it('should return ID as string when entity not found', () => {
    const result = getEntityNameById(
      99,
      testEntities,
      (e) => e.id,
      (e) => e.name
    );
    expect(result).toBe('99');
  });

  it('should return null when id is null', () => {
    const result = getEntityNameById(
      null,
      testEntities,
      (e) => e.id,
      (e) => e.name
    );
    expect(result).toBeNull();
  });

  it('should return ID as string when entities array is empty', () => {
    const result = getEntityNameById(
      1,
      [],
      (e) => e.id,
      (e) => e.name
    );
    expect(result).toBe('1');
  });

  it('should handle entities with different structure', () => {
    interface DifferentEntity {
      entityId: number;
      entityName: string;
    }

    const differentEntities: DifferentEntity[] = [
      { entityId: 10, entityName: 'Forma X' },
      { entityId: 20, entityName: 'Forma Y' },
    ];

    const result = getEntityNameById(
      10,
      differentEntities,
      (e) => e.entityId,
      (e) => e.entityName
    );
    expect(result).toBe('Forma X');
  });

  it('should return first matching entity when multiple entities have same ID', () => {
    const duplicateEntities: TestEntity[] = [
      { id: 1, name: 'First' },
      { id: 1, name: 'Second' },
    ];

    const result = getEntityNameById(
      1,
      duplicateEntities,
      (e) => e.id,
      (e) => e.name
    );
    expect(result).toBe('First');
  });
});

