# Departments API Documentation

## Base URL
`/api/v1/admin/departments`

## Authentication
Required Role: `ADMIN`

---

## 1. Get All Departments
Retrieves a list of all departments in the system. Flat list structure, including `parentId`.

**Endpoint:** `GET /`

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": 1,
      "code": "BOD_L1",
      "name": "Ban Giám Đốc (BOD)",
      "parentId": null,
      "createdAt": "2026-07-06T10:00:00",
      "updatedAt": "2026-07-06T10:00:00"
    },
    {
      "id": 2,
      "code": "BOD",
      "name": "Ban Giám Đốc",
      "parentId": 1,
      "createdAt": "2026-07-06T10:00:00",
      "updatedAt": "2026-07-06T10:00:00"
    }
  ]
}
```

---

## 2. Create Department
Creates a new department.

**Endpoint:** `POST /`

**Request Body:**
```json
{
  "code": "HR",
  "name": "Human Resources",
  "parentId": null // (Optional) ID of parent department
}
```

**Response (200 OK):**
```json
{
  "data": {
      "id": 3,
      "code": "HR",
      "name": "Human Resources",
      "parentId": null,
      "createdAt": "2026-07-06T10:00:00",
      "updatedAt": "2026-07-06T10:00:00"
  }
}
```

**Error (400 Bad Request):**
- Department code already exists.
- Parent department not found.

---

## 3. Update Department
Updates an existing department's details.

**Endpoint:** `PUT /{id}`

**Request Body:**
```json
{
  "code": "HR-NEW",
  "name": "Human Resources Updated",
  "parentId": null
}
```

**Response (200 OK):**
```json
{
  "data": {
      "id": 3,
      "code": "HR-NEW",
      "name": "Human Resources Updated",
      "parentId": null,
      "createdAt": "2026-07-06T10:00:00",
      "updatedAt": "2026-07-06T10:05:00"
  }
}
```

**Error (400 Bad Request):**
- Department not found.
- Department code already exists.
- Department cannot be its own parent.

---

## 4. Delete Department
Deletes a department by ID.

**Endpoint:** `DELETE /{id}`

**Response (200 OK):**
```json
{
  "message": "Department deleted successfully"
}
```

**Error (400 Bad Request):**
- Cannot delete department because it has child departments.
