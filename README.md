# placePicker
  usage  
 implementation 'com.github.varsha777:placePicker:1.7'

# To start place picker activity

 Intent intent=new Intent(this,PlacePickMap.class);
 intent.putExtra(PlacePickMap.PlacePickerConstants.GOOGLE_MAPS_KEY,getString(R.string.google_maps_key));//// google maps key is manditory
 startActivityForResult(intent,REQUEST_CODE);

# Result

   @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1234) {
               double latitude=data.getDoubleExtra(PlacePickMap.PlacePickerConstants.SELECT_LOCATION_LATITUDE, 0.0);
               double longitude=data.getDoubleExtra(PlacePickMap.PlacePickerConstants.SELECT_LOCATION_LONGITUDE, 0.0);
               String address=data.getStringExtra(PlacePickMap.PlacePickerConstants.PLACE_PICKER_ADDRESS);
            }
        }
    }
    
    
