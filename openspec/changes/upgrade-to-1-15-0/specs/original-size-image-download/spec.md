## MODIFIED Requirements

### Requirement: The app configures the downloaded image size
The SDK configuration SHALL accept an image dimension (small, medium, large or
original) for file resource downloads. When the app configures one, the SDK
SHALL request every image file resource at that dimension instead of choosing
the size itself. When the app does not configure one, the SDK SHALL keep the
stock SDK's own image size selection.

#### Scenario: No dimension configured
- **WHEN** the app builds the SDK configuration without an image dimension
- **THEN** image file resources are downloaded at the size the stock SDK selects for them, as if the customization did not exist

#### Scenario: Dimension configured
- **WHEN** the app configures the small, medium or large dimension
- **THEN** image file resources are requested from the server at that dimension, whatever size the stock SDK would have selected

#### Scenario: Configured dimension larger than the stock selection
- **WHEN** the app configures a dimension larger than the medium size the stock SDK caps images at
- **THEN** image file resources are still requested at the configured dimension

### Requirement: Original images are downloaded without resizing
When the configured dimension is original, the SDK SHALL request image file
resources at the server's original dimension, so the server returns the
original file.

#### Scenario: Original dimension configured
- **WHEN** the app configures the original dimension and the SDK downloads an image file resource
- **THEN** the request asks for the original dimension
- **AND** the stored file is the original uploaded file

#### Scenario: Non-image file resources
- **WHEN** the SDK downloads a file resource that is not an image
- **THEN** the configured dimension does not change how it is downloaded
