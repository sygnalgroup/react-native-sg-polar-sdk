import * as React from 'react';

import { StyleSheet, View, Text, NativeModules } from 'react-native';

const { SgPolarSdk } = NativeModules;

export default function App() {
  console.log(SgPolarSdk);

  return (
    <View style={styles.container}>
      <Text>Link!</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
