# Original Size Image Download

## Purpose
The stock SDK always downloads tracker and aggregated image values resized by
the server, so clients that need the full picture (documents, clinical photos)
only get a reduced copy on the device. This customization lets the app choose,
once for the whole SDK, the image size to download, including the original
file without resizing.

## Requirements

### Requirement: The app configures the downloaded image size
The SDK configuration SHALL accept an image dimension (small, medium, large or
original) for file resource downloads, and SHALL default to medium when the
app does not set one.

#### Scenario: No dimension configured
- **WHEN** the app builds the SDK configuration without an image dimension
- **THEN** image file resources are downloaded at the medium dimension

#### Scenario: Dimension configured
- **WHEN** the app configures the small, medium or large dimension
- **THEN** image file resources are requested from the server at that dimension

### Requirement: Original images are downloaded without resizing
When the configured dimension is original, the SDK SHALL request image file
resources without a dimension, so the server returns the original file.

#### Scenario: Original dimension configured
- **WHEN** the app configures the original dimension and the SDK downloads an image file resource
- **THEN** the request carries no dimension
- **AND** the stored file is the original uploaded file

#### Scenario: Non-image file resources
- **WHEN** the SDK downloads a file resource that is not an image
- **THEN** the configured dimension does not change how it is downloaded
