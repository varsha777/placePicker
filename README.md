# placePicker

# To start place picker activity

Intent intent=new Intent(this,PlacePickMap.class);
intent.putExtra("GOOGLE_MAPS_KEY",getString(R.string.google_maps_key));//// google maps key is manditory
startActivityForResult(intent,REQUEST_CODE);

# Result

   @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1234) {
               double latitude=data.getDoubleExtra("SELECT_LOCATION_LATITUDE", 0.0);
               double longitude=data.getDoubleExtra("SELECT_LOCATION_LONGITUDE", 0.0);
               Strng address=data.getStringExtra("PLACE_PICKER_ADDRESS");
            }
        }
    }
    
    
